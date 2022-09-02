package com.ormanager.orm;

import com.ormanager.jdbc.ConnectionToDB;
import com.ormanager.orm.annotation.*;
import com.ormanager.orm.exception.OrmFieldTypeException;
import com.ormanager.orm.mapper.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;

@Slf4j(topic = "OrmManager")
public class OrmManager<T> {
    private java.sql.Connection con;

    public static <T> OrmManager<T> withPropertiesFrom(String filename) throws SQLException {
        ConnectionToDB.setFileName(filename);
        return new OrmManager<T>(ConnectionToDB.getConnection());
    }

    public static <T> OrmManager<T> getConnectionWithArgmunets(String url, String username, String password) throws SQLException {
        return new OrmManager<T>(url, username, password);
    }

    public static <T> OrmManager<T> withDataSource(DataSource dataSource) throws SQLException {
        return new OrmManager<T>(dataSource.getConnection());
    }

    private OrmManager(Connection connection) {
        this.con = connection;
    }

    private OrmManager(String url, String username, String password) throws SQLException {
        this.con = DriverManager.
                getConnection(url, username, password);
    }

    public void persist(T t) throws SQLException, IllegalAccessException {
        String sqlStatement = getInsertStatement(t);

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            mapStatement(t, preparedStatement);
        }
    }

    public T save(T t) throws SQLException, IllegalAccessException {
        String sqlStatement = getInsertStatement(t);

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

    private String getInsertStatement(T t) {
        var length = getAllColumnsButIdAndOneToMany(t);
        var questionMarks = IntStream.range(0, length.intValue())
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
        return sqlStatement;
    }

    public boolean merge(T entity) {
        boolean isMerged = false;

        if (isRecordInDataBase(entity)) {
            String queryCheck = String.format("UPDATE %s SET %s WHERE id = ?",
                    getTableClassName(entity), getColumnFieldsWithValuesToString(entity));

            try (PreparedStatement preparedStatement = con.prepareStatement(queryCheck)) {
                preparedStatement.setString(1, getRecordId(entity));
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isMerged = preparedStatement.executeUpdate() > 0;
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return isMerged;
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
            else if (!field.isAnnotationPresent(OneToMany.class)){
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
        return String.join(",", getAllValuesFromObject(t));
    }

    public List<String> getAllValuesFromObject(T t) {
        List<String> strings = new ArrayList<>();
        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            if (field.isAnnotationPresent(Column.class)) {
                if (!Objects.equals(field.getDeclaredAnnotation(Column.class).name(), "")) {
                    strings.add(field.getDeclaredAnnotation(Column.class).name());
                } else {
                    strings.add(field.getName());
                }
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                strings.add(field.getDeclaredAnnotation(ManyToOne.class).columnName());
            } else if (!Collection.class.isAssignableFrom(field.getType()) && !field.isAnnotationPresent(Id.class)) {
                strings.add(field.getName());
            }
        }
        return strings;
    }

    public String getColumnFieldsWithValuesToString(T t) {
        try {
            return String.join(", ", getColumnFieldsWithValues(t));
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    public List<String> getColumnFieldsWithValues(T t) throws IllegalAccessException {
        List<String> strings = new ArrayList<>();

        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Column.class)) {
                if (!Objects.equals(field.getDeclaredAnnotation(Column.class).name(), "")) {
                    strings.add(field.getDeclaredAnnotation(Column.class).name() + "='" + field.get(t) + "'");
                } else {
                    strings.add(field.getName() + "='" + field.get(t) + "'");
                }
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                String recordId = getRecordId(field.get(t));
                strings.add(field.getDeclaredAnnotation(ManyToOne.class).columnName() + "='" + recordId + "'");
            }
        }
        return strings;
    }

    public List<Field> getAllColumnsButId(T t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    public Long getAllColumnsButIdAndOneToMany(T t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .filter(v -> !v.isAnnotationPresent(OneToMany.class))
                .count();
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

    public Iterable<T> findAllAsIterable(Class<T> cls) {
        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
        LOGGER.info("sqlStatement {}", sqlStatement);
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            return () -> new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    try {
                        return resultSet.next();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public T next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    T t = null;
                    try {
                        t = cls.getConstructor().newInstance();
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                             NoSuchMethodException e) {
                        LOGGER.info(String.valueOf(e));
                    }
                    ObjectMapper.mapperToObject(resultSet, t);
                    return t;
                }
            };
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

        var basicFields = getBasicFieldsFromClass(clazz);

        var fieldsAndTypes = new StringBuilder();

        for (var basicField : basicFields) {
            var sqlTypeForField = getSqlTypeForField(basicField);

            if (basicField.isAnnotationPresent(Column.class)) {
                if (basicField.getAnnotation(Column.class).name().equals("")) {
                    fieldsAndTypes.append(" ").append(basicField.getName());
                } else {
                    fieldsAndTypes.append(" ").append(basicField.getAnnotation(Column.class).name());
                }
            } else {
                fieldsAndTypes.append(" ").append(basicField.getName());
            }
            fieldsAndTypes.append(sqlTypeForField);
        }

        StringBuilder registerSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (" + id.getName() + " BIGINT UNSIGNED AUTO_INCREMENT,"
                + fieldsAndTypes + " PRIMARY KEY (" + id.getName() + "))");

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + registerSQL);

        try (PreparedStatement preparedStatement = con.prepareStatement(String.valueOf(registerSQL))) {
            preparedStatement.execute();

            LOGGER.info("CREATE TABLE SQL completed successfully! {} entity has been created in DB.", tableName.toUpperCase());
        }
    }

    public void createRelationships(Class<?>... entityClasses) throws SQLException {
        for (var entity : entityClasses) {
            if (doesClassHaveAnyRelationship(entity)) {
                createRelationships(entity);
            }
        }
    }

    private void createRelationships(Class<?> clazz) throws SQLException {
        for (var field : getRelationshipFields(clazz, ManyToOne.class)) {

            var fieldClass = field.getType();
            var fieldTableAnnotationClassName = getTableName(fieldClass);
            var fieldBasicClassNameWithId = fieldClass.getSimpleName().toLowerCase() + "_id";
            var fieldClassIdName = getIdField(fieldClass).getName();

            if (doesEntityExists(fieldClass) && !(doesRelationshipAlreadyExist(clazz, fieldClass))) {

                var relationshipSQL = "ALTER TABLE " + getTableName(clazz) + " ADD COLUMN " + fieldBasicClassNameWithId + " BIGINT UNSIGNED," +
                        " ADD FOREIGN KEY (" + fieldBasicClassNameWithId + ")" +
                        " REFERENCES " + fieldTableAnnotationClassName + "(" + fieldClassIdName + ") ON DELETE CASCADE;";

                LOGGER.info("Establishing relationship between entities: {} and {} is being processed now: " + relationshipSQL, clazz.getSimpleName().toUpperCase(), fieldClass.getSimpleName().toUpperCase());

                try (PreparedStatement statement = con.prepareStatement(relationshipSQL)) {
                    statement.execute();

                    LOGGER.info("Establishing relationship processed successfully!");
                }
            } else {
                if (!doesEntityExists(fieldClass)) {
                    var missingEntityName = fieldClass.getSimpleName();

                    throw new SQLException(String.format("Relationship between %s and %s cannot be made! Missing entity %s!", clazz.getSimpleName(), missingEntityName, missingEntityName));
                }
                LOGGER.info("Relationship between entities: {} and {} already exists.", clazz.getSimpleName().toUpperCase(), fieldClass.getSimpleName().toUpperCase());
            }
        }
    }

    private String getSqlTypeForField(Field field) {
        var fieldType = field.getType();

        if (fieldType == String.class) {
            return " VARCHAR(255),";
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return " INT,";
        } else if (fieldType == LocalDate.class) {
            return " DATE,";
        } else if (fieldType == LocalTime.class) {
            return " DATETIME,";
        } else if (fieldType == UUID.class) {
            return " UUID,";
        } else if (fieldType == long.class || fieldType == Long.class) {
            return " BIGINT,";
        } else if (fieldType == double.class || fieldType == Double.class) {
            return " DOUBLE,";
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return " BOOLEAN,";
        } else if (fieldType == BigDecimal.class) {
            return " BIGINT,";
        }
        throw new OrmFieldTypeException("Could not get sql type for given field: " + fieldType);
    }

    private String getTableName(Class<?> clazz) {
        var tableAnnotation = Optional.ofNullable(clazz.getAnnotation(Table.class));

        return tableAnnotation.isPresent() ? tableAnnotation.get().name() : clazz.getSimpleName().toLowerCase();
    }

    private List<Field> getBasicFieldsFromClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .filter(field -> !field.isAnnotationPresent(ManyToOne.class))
                .filter(field -> field.getType() != Collection.class)
                .toList();
    }

    private Field getIdField(Class<?> clazz) throws SQLException {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new SQLException(String.format("ID field not found in class %s !", clazz)));
    }

    private List<Field> getRelationshipFields(Class<?> clazz, Class<? extends Annotation> relationAnnotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(relationAnnotation))
                .toList();
    }

    private boolean doesRelationshipAlreadyExist(Class<?> clazzToCheck, Class<?> relationToCheck) throws SQLException {
        String findRelationSQL = "SELECT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = '" + getTableName(clazzToCheck) + "';";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(findRelationSQL);
            resultSet.next();

            while (resultSet.next()) {
                if (resultSet.getString(1).equals(getTableName(relationToCheck))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doesClassHaveAnyRelationship(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(ManyToOne.class));
    }

    private boolean doesEntityExists(Class<?> clazz) throws SQLException {
        var searchedEntityName = getTableName(clazz);

        String checkIfEntityExistsSQL = "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE (TABLE_SCHEMA = 'test') AND (TABLE_NAME = '" + searchedEntityName + "');";

        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(checkIfEntityExistsSQL);
            resultSet.next();

            return resultSet.getInt(1) == 1;
        }
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

    private String getRecordId(Object recordInDb) throws IllegalAccessException {
        Optional<Field> optionalId = Arrays.stream(recordInDb.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            Object o = optionalId.get().get(recordInDb);
            return o != null ? o.toString() : "";
        }
        return "";
    }

    public Object update(T o) throws IllegalAccessException {
        if (getId(o) != null && isRecordInDataBase(o)) {
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
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            return (Serializable) optionalId.get().get(o);
        }
        return null;
    }
}
