package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.orm.annotation.*;
import com.ormanager.orm.exception.DataConnectionException;
import com.ormanager.orm.exception.OrmFieldTypeException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class OrmManager<T> {

    PropertiesConfiguration prop = new PropertiesConfiguration("src/main/resources/application.properties");

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmManager.class);

    private Connection conn;

    public static <T> OrmManager<T> getConnection() throws SQLException, ConfigurationException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException, ConfigurationException {
        this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", prop.getString("dataSource.password"));
    }

    void register(Class<?>... entityClasses) throws SQLException, DataConnectionException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    public void register(Class<?> clazz) throws DataConnectionException, SQLException {

        var tableName = getTableName(clazz);

        var id = getIdField(clazz);

        var fieldsMarkedAsColumn = getColumnFields(clazz);

        var columnNamesAndTypes = new StringBuilder();

        for (var fieldAsColumn : fieldsMarkedAsColumn) {
            var columnAnnotationDescribedName = fieldAsColumn.getAnnotation(Column.class).name();
            var sqlTypeForField = getSqlTypeForField(fieldAsColumn);

            if (columnAnnotationDescribedName.equals("")) {
                columnNamesAndTypes.append(" ").append(fieldAsColumn.getName());
            } else {
                columnNamesAndTypes.append(" ").append(columnAnnotationDescribedName);
            }
            columnNamesAndTypes.append(sqlTypeForField);
        }

        StringBuilder registerSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName +
                                                          " (" + id.getName() + " int UNSIGNED AUTO_INCREMENT,"
                                                          + columnNamesAndTypes
                                                          + " PRIMARY KEY (" + id.getName() + ")");

        if (doesClassHaveAnyRelationship(clazz)) {
            createAssociatedTablesForManyToOneRelationshipAndUpdateRegisterQuery(clazz, registerSQL);
        }

        registerSQL.append(")");

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + registerSQL);

        PreparedStatement preparedStatement = conn.prepareStatement(String.valueOf(registerSQL));
        preparedStatement.execute();

        LOGGER.info("CREATE TABLE SQL completed successfully! {} entity has been created in DB.", tableName.toUpperCase());
    }

    private String getSqlTypeForField(Field field) {
        field.setAccessible(true);

        var fieldType = field.getType();

        if (fieldType == String.class) {
            return " VARCHAR(255),";
        } else if (fieldType == int.class) {
            return " INT,";
        } else if (fieldType == LocalDate.class) {
            return " DATE,";
        }
        throw new OrmFieldTypeException("Could not get sql type for given field: " + fieldType);
    }

    private List<Field> getRelationshipFieldsIfExist(Class<?> clazz, Class<? extends Annotation> relationAnnotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(relationAnnotation))
                .toList();
    }

    private String getTableName(Class<?> clazz) {
        var tableAnnotation = Optional.ofNullable(clazz.getAnnotation(Table.class));

        return tableAnnotation.isPresent() ? tableAnnotation.get().name() : clazz.getSimpleName().toLowerCase();
    }

    private List<Field> getColumnFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .toList();
    }

    private Field getIdField(Class<?> clazz) throws SQLException {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new SQLException(String.format("ID field not found in class %s !", clazz)));
    }

    private void createAssociatedTablesForManyToOneRelationshipAndUpdateRegisterQuery(Class<?> clazz, StringBuilder query) throws DataConnectionException, SQLException {
        for (var field : getRelationshipFieldsIfExist(clazz, ManyToOne.class)) {

            if (!doesAssociatedTableAlreadyExists(field.getType())) {
                register(field.getType());
            }

            expandRegisterQueryForRelationField(query, field);
        }
    }

    private boolean doesAssociatedTableAlreadyExists(Class<?> clazz) throws SQLException {
        var searchedTableName = getTableName(clazz);

        String checkIfEntityExistsSQL = "SELECT COUNT(*) FROM information_schema.TABLES " +
                                        "WHERE (TABLE_SCHEMA = 'test') AND (TABLE_NAME = '" + searchedTableName + "');";

        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(checkIfEntityExistsSQL);
        resultSet.next();

        return resultSet.getInt(1) == 1;
    }

    private boolean doesClassHaveAnyRelationship(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(ManyToOne.class));
    }

    private void expandRegisterQueryForRelationField(StringBuilder query, Field field) throws SQLException {
        var associatedClassName = getTableName(field.getType());
        var associatedClassIdFieldName = getIdField(field.getType()).getName();

        var strToAppend = ", " + associatedClassName + "_id INT UNSIGNED, " +
                          "FOREIGN KEY (" + associatedClassName + "_id) " +
                          "REFERENCES " + associatedClassName + "(" + associatedClassIdFieldName + ")";

        query.append(strToAppend);
    }

    public static void main(String[] args) throws SQLException, DataConnectionException, ConfigurationException {
        OrmManager.getConnection().register(Book.class);
    }
}
