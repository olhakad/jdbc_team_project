package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import com.ormanager.orm.exception.OrmFieldTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class OrmManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmManager.class);

    private Connection conn;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "testUser", "test");
    }

    void register(Class<?>... entityClasses) throws SQLException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    public void register(Class<?> clazz) throws SQLException {

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

    public static void main(String[] args) throws SQLException {
        OrmManager.getConnection().register(Book.class);
    }
}
