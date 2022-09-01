package com.ormanager.orm;

import com.ormanager.jdbc.DataSource;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import com.ormanager.orm.exception.OrmFieldTypeException;
import com.ormanager.orm.mapper.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;

@Slf4j(topic = "OrmManager")
public class OrmManager<T> {
    private Connection con;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DataSource.getConnection();
    }

    public void persist(T t) throws SQLException, IllegalAccessException {
        var length = getAllDeclaredFieldsFromObject(t).size() - 1;
        var questionMarks = IntStream.range(0, length)
                .mapToObj(q -> "?")
                .collect(Collectors.joining(","));

        String sqlStatement = "INSERT INTO "
                .concat(getTableClassName(t))
                .concat("(")
                .concat(getAllValuesFromListToString(t))
                .concat(") VALUES(")
                .concat(questionMarks)
                .concat(");");

        LOGGER.info("SQL STATEMENT : {}", sqlStatement);

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            mapStatement(t, preparedStatement);
        }
    }


    public T save(T t) throws SQLException, IllegalAccessException {
        var length = getAllDeclaredFieldsFromObject(t).size() - 1;
        var questionMarks = IntStream.range(0, length)
                .mapToObj(q -> "?")
                .collect(Collectors.joining(","));

        String sqlStatement = "INSERT INTO "
                .concat(getTableClassName(t))
                .concat("(")
                .concat(getAllValuesFromListToString(t))
                .concat(") VALUES(")
                .concat(questionMarks)
                .concat(");");

        LOGGER.info("SQL STATEMENT : {}", sqlStatement);

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS)) {
            mapStatement(t, preparedStatement);
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            long id = -1;
            while (generatedKeys.next()) {
                for (Field field : getAllDeclaredFieldsFromObject(t)) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        id = generatedKeys.getLong(1);
                        field.set(t, id);
                    }
                }
            }
            return t;
        }
    }

    private void mapStatement(T t, PreparedStatement preparedStatement) throws SQLException, IllegalAccessException {
        for (Field field : getAllColumnsButId(t)) {
            field.setAccessible(true);
            var index = getAllColumnsButId(t).indexOf(field) + 1;
            if (field.getType() == String.class) {
                preparedStatement.setString(index, (String) field.get(t));
            } else if (field.getType() == LocalDate.class) {
                Date date = Date.valueOf((LocalDate) field.get(t));
                preparedStatement.setDate(index, date);
            }
            //if we don't pass the value / don't have mapped type
            else {
                preparedStatement.setObject(index, null);
            }
        }
        LOGGER.info("PREPARED STATEMENT : {}", preparedStatement);
        preparedStatement.executeUpdate();
    }

    public String getTableClassName(T t) {
        return t.getClass().getAnnotation(Table.class).name();
    }

    public List<Field> getAllDeclaredFieldsFromObject(T t) {
        return Arrays.asList(t.getClass().getDeclaredFields());
    }

    public String getAllValuesFromListToString(T t) {
        return getAllValuesFromObject(t).stream().collect(Collectors.joining(","));
    }

    public List<String> getAllValuesFromObject(T t) {
        List<String> strings = new ArrayList<>();
        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            if (!field.isAnnotationPresent(Id.class)) {
                if (field.isAnnotationPresent(Column.class)
                        && !Objects.equals(field.getDeclaredAnnotation(Column.class).name(), "")) {
                    strings.add(field.getDeclaredAnnotation(Column.class).name());
                } else {
                    strings.add(field.getName());
                }
            }
        }
        return strings;
    }

    public List<Field> getAllColumnsButId(T t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        T t = null;
        String sqlStatement = "SELECT * FROM "
                .concat(cls.getDeclaredAnnotation(Table.class).name())
                .concat(" WHERE id=")
                .concat(id.toString())
                .concat(";");
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            t = cls.getDeclaredConstructor().newInstance();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                t = mapperToObject(resultSet, t).orElseThrow();
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            LOGGER.info(String.valueOf(e));
        }
        return Optional.ofNullable(t);
    }

    public List<T> findAll(Class<T> cls) throws SQLException {
        List<T> allEntities = new ArrayList<>();
        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
        LOGGER.info("sqlStatement {}", sqlStatement);
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                T t = cls.getConstructor().newInstance();
                ObjectMapper.mapperToObject(resultSet, t);
                allEntities.add(t);
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                 NoSuchMethodException e) {
            LOGGER.info(String.valueOf(e));
        }
        return allEntities;
    }

    public Stream<T> findAllAsStream(Class<T> cls) {
        try {
            return findAll(cls).stream();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public boolean delete(T recordToDelete) {
        boolean isDeleted = false;
        if (isRecordInDataBase(recordToDelete)) {
            String tableName = recordToDelete.getClass().getAnnotation(Table.class).name();
            String queryCheck = String.format("DELETE FROM %s WHERE id = ?", tableName);

            try (PreparedStatement preparedStatement = con.prepareStatement(queryCheck)) {
                String recordId = getRecordId(recordToDelete);
                preparedStatement.setString(1, recordId);
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isDeleted = preparedStatement.executeUpdate() > 0;
            } catch (SQLException | IllegalAccessException e) {
                LOGGER.error(e.getMessage());
            }

            if (isDeleted) {
                setObjectToNull(recordToDelete);
            }
        }
        return isDeleted;
    }

    private void setObjectToNull(T targetObject) {
        Arrays.stream(targetObject.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            try {
                field.set(targetObject, null);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage());
            }
        });
    }

    private boolean isRecordInDataBase(T searchedRecord) {
        boolean isInDB = false;
        String tableName = searchedRecord.getClass().getAnnotation(Table.class).name();
        String queryCheck = String.format("SELECT count(*) FROM %s WHERE id = ?", tableName);

        try (PreparedStatement preparedStatement = con.prepareStatement(queryCheck)) {
            String recordId = getRecordId(searchedRecord);

            preparedStatement.setString(1, recordId);
            LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                isInDB = count == 1;
            }
        } catch (SQLException | IllegalAccessException e) {
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("This {} {} in Data Base.",
                searchedRecord.getClass().getSimpleName(),
                isInDB ? "exists" : "does not exist");

        return isInDB;
    }

    private String getRecordId(T recordInDb) throws IllegalAccessException {
        Optional<Field> optionalId = Arrays.stream(recordInDb.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            return optionalId.get().get(recordInDb).toString();
        }
        return "";
    }

    public Object update(T o) throws IllegalAccessException {
        if(getId(o)!=null && isRecordInDataBase(o)) {
            LOGGER.info("This {} has been updated from Data Base.",
                    o.getClass().getSimpleName());
            return findById(getId(o), o.getClass()).get();
        }
        LOGGER.info("There is no such object with id in database or id of element is null.");
        LOGGER.info("The object {} that was passed to the method was returned.",
                o.getClass().getSimpleName());
        return o;
    }

    private Serializable getId(T o) throws IllegalAccessException {

        Optional<Field> optionalId = Arrays.stream(o.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
        if (optionalId.isPresent() && optionalId != null) {
            optionalId.get().setAccessible(true);
            return (Serializable) optionalId.get().get(o);
        }
        return null;
    }
}
