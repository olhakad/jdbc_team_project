package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.*;
import com.ormanager.orm.exception.DataConnectionException;
import com.ormanager.orm.exception.OrmFieldTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OrmManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmManager.class);

    private Connection conn;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
//        this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "");
    }

    void register(Class<?>... entityClasses) throws SQLException, DataConnectionException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    public void register(Class<?> clazz) throws SQLException, DataConnectionException {

        var id = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new SQLException("ID field not found!"));

        var fieldsMarkedAsColumn = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .toList();

        var columnNames = new StringBuilder();

        for (var fieldAsColumn : fieldsMarkedAsColumn) {
            var columnAnnotationDescribedName = fieldAsColumn.getAnnotation(Column.class).name();
            var sqlTypeForField = getSqlTypeForField(fieldAsColumn);

            if (columnAnnotationDescribedName.equals("")) {
                columnNames.append(fieldAsColumn.getName());
            } else {
                columnNames.append(columnAnnotationDescribedName);
            }
            columnNames.append(sqlTypeForField);
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + getTableName(clazz) + " (" + id.getName() + " UNSIGNED AUTO_INCREMENT PRIMARY KEY, " + columnNames);

        if (getRelationshipFieldsIfExist(clazz, ManyToOne.class).size() != 0) {
            processManyToOneRelationship(clazz);
            sql.append(" publisher_id int REFERENCES publisher(id)");
        }

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + sql);

//        PreparedStatement preparedStatement = conn.prepareStatement(sql);
//        preparedStatement.execute();
    }

    private String getTableName(Class<?> clazz) {
        var tableAnnotation = Optional.ofNullable(clazz.getAnnotation(Table.class));

        return tableAnnotation.isPresent() ? tableAnnotation.get().name() : clazz.getSimpleName().toLowerCase();
    }

    private String getSqlTypeForField(Field field) {
        field.setAccessible(true);

        var fieldType = field.getType();

        if (fieldType == String.class) {
            return " VARCHAR(255), ";
        } else if (fieldType == int.class) {
            return " INT, ";
        } else if (fieldType == LocalDate.class) {
            return " DATE, ";
        }
        throw new OrmFieldTypeException("Could not get sql type for given field: " + fieldType);
    }

    private Set<Field> getRelationshipFieldsIfExist(Class<?> clazz, Class<? extends Annotation> relationAnnotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(relationAnnotation))
                .collect(Collectors.toSet());
    }

    private void processManyToOneRelationship(Class<?> clazz) throws DataConnectionException, SQLException {
        for (var field : getRelationshipFieldsIfExist(clazz, ManyToOne.class)) {
            register(field.getType());
        }
    }

    public static void main(String[] args) throws SQLException, DataConnectionException {
        OrmManager.getConnection().register(Book.class);
    }
}
