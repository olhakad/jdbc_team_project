package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import com.ormanager.orm.exception.OrmFieldTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrmManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmManager.class);

    private Connection con;
    private AtomicLong id = new AtomicLong(0L);
    private int idIndex = 1;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
    }

    public void persist(T t) throws IllegalArgumentException, SQLException, IllegalAccessException {
        Class<?> clazz = t.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();
        Field pk = null;
        ArrayList<Field> columns = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(",");
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class)) {
                pk = field;
            } else {
                joiner.add(field.getName());
                columns.add(field);
            }
        }
        int length = columns.size() + 1;
        String qMarks = IntStream.range(0, length)
                .mapToObj(e -> "?")
                .collect(Collectors.joining(","));
        String sql = "INSERT INTO " + clazz.getSimpleName() + "( " + pk.getName() + "," + joiner.toString() + ") " + "values (" + qMarks + ")";
        System.out.println(sql);
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        if (pk.getType() == Long.class) {
            preparedStatement.setLong(idIndex, id.incrementAndGet());
        }
        idIndex++;
        for (Field field : columns) {
            field.setAccessible(true);
            if (field.getType() == String.class) {
                preparedStatement.setString(idIndex++, (String) field.get(t));
            } else if (field.getType() == int.class) {
                preparedStatement.setInt(idIndex++, (int) field.get(t));
            } else if (field.getType() == LocalDate.class) {
                Date date = Date.valueOf((LocalDate) field.get(t));
                preparedStatement.setDate(idIndex++, date);
            }
        }
        preparedStatement.executeUpdate();
    }
    
    public void register(Class<?>... entityClasses) throws SQLException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    private void register(Class<?> clazz) throws SQLException {
        if (doesEntityExists(clazz)) {
            LOGGER.info("{} already exists in database!", clazz.getSimpleName());
            return;
        }

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
                                                          + " PRIMARY KEY (" + id.getName() + "))");

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + registerSQL);

        PreparedStatement preparedStatement = con.prepareStatement(String.valueOf(registerSQL));
        preparedStatement.execute();

        LOGGER.info("CREATE TABLE SQL completed successfully! {} entity has been created in DB.", tableName.toUpperCase());
    }

    private String getSqlTypeForField(Field field) {
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

    private boolean doesEntityExists(Class<?> clazz) throws SQLException {
        var searchedEntityName = getTableName(clazz);

        String checkIfEntityExistsSQL = "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE (TABLE_SCHEMA = 'test') AND (TABLE_NAME = '" + searchedEntityName + "');";

        Statement statement = con.createStatement();
        ResultSet resultSet = statement.executeQuery(checkIfEntityExistsSQL);
        resultSet.next();

        return resultSet.getInt(1) == 1;
}
