package com.ormanager.orm;

import com.ormanager.jdbc.ConnectionToDB;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.Table;
import com.ormanager.orm.mapper.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;

@Slf4j(topic = "OrmManager")
public class OrmManager {
    private final Cache ormCache;

    private final OrmManagerUtil ormManagerUtil;
    private final Connection connection;

    public static OrmManager withPropertiesFrom(String filename) throws SQLException {
        ConnectionToDB.setFileName(filename);
        return new OrmManager(ConnectionToDB.getConnection());
    }

    public static OrmManager getConnectionWithArguments(String url, String username, String password) throws SQLException {
        return new OrmManager(url, username, password);
    }

    public static OrmManager withDataSource(DataSource dataSource) throws SQLException {
        return new OrmManager(dataSource.getConnection());
    }

    private OrmManager(Connection connection) {
        ormManagerUtil = new OrmManagerUtil();
        this.connection = connection;
        ormCache = new Cache();
    }

    private OrmManager(String url, String username, String password) throws SQLException {
        ormManagerUtil = new OrmManagerUtil();
        this.connection = DriverManager.
                getConnection(url, username, password);
        ormCache = new Cache();
    }

    public void register(Class<?>... entityClasses) throws SQLException, NoSuchFieldException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    void register(Class<?> clazz) throws SQLException, NoSuchFieldException {
        if (doesEntityExist(clazz)) {
            LOGGER.info("{} already exists in database!", clazz.getSimpleName());
            return;
        }

        var tableName = ormManagerUtil.getTableName(clazz);

        var idFieldName = ormManagerUtil.getIdFieldName(clazz);

        var basicFields = ormManagerUtil.getBasicFieldsFromClass(clazz);

        var fieldsAndTypes = new StringBuilder();

        for (var basicField : basicFields) {
            var sqlTypeForField = ormManagerUtil.getSqlTypeForField(basicField);

            if (basicField.isAnnotationPresent(Column.class) && !basicField.getAnnotation(Column.class).name().equals("")) {
                fieldsAndTypes.append(" ").append(basicField.getAnnotation(Column.class).name());
            } else {
                fieldsAndTypes.append(" ").append(basicField.getName());
            }
            fieldsAndTypes.append(sqlTypeForField);
        }

        StringBuilder registerSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (" + idFieldName + " BIGINT UNSIGNED AUTO_INCREMENT,"
                + fieldsAndTypes + " PRIMARY KEY (" + idFieldName + "))");

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + registerSQL);

        try (PreparedStatement preparedStatement = connection.prepareStatement(String.valueOf(registerSQL))) {
            preparedStatement.execute();

            LOGGER.info("CREATE TABLE SQL completed successfully! {} entity has been created in DB.", tableName.toUpperCase());
        }
    }

    public void createRelationships(Class<?>... entityClasses) throws SQLException, NoSuchFieldException {
        for (var entity : entityClasses) {
            if (OrmManagerUtil.doesClassHaveGivenRelationship(entity, ManyToOne.class)) {
                createRelationships(entity);
            }
        }
    }

    void createRelationships(Class<?> clazz) throws SQLException, NoSuchFieldException {
        for (var field : OrmManagerUtil.getRelationshipFields(clazz, ManyToOne.class)) {

            var fieldClass = field.getType();
            var fieldTableAnnotationClassName = ormManagerUtil.getTableName(fieldClass);
            var fieldNameFromManyToOneAnnotation = field.getAnnotation(ManyToOne.class).columnName();
            var fieldName = fieldNameFromManyToOneAnnotation.equals("") ? fieldClass.getSimpleName().toLowerCase() + "_id" : fieldNameFromManyToOneAnnotation;
            var fieldClassIdName = ormManagerUtil.getIdFieldName(fieldClass);
            var clazzTableName = ormManagerUtil.getTableName(clazz);

            if (doesEntityExist(clazz) && doesEntityExist(fieldClass) && !(doesRelationshipAlreadyExist(clazz, fieldClass))) {

                var relationshipSQL = "ALTER TABLE " + clazzTableName + " ADD COLUMN " + fieldName + " BIGINT UNSIGNED," +
                        " ADD FOREIGN KEY (" + fieldName + ")" +
                        " REFERENCES " + fieldTableAnnotationClassName + "(" + fieldClassIdName + ") ON DELETE CASCADE;";

                LOGGER.info("Establishing relationship between entities: {} and {} is being processed now: " + relationshipSQL, clazz.getSimpleName().toUpperCase(), fieldClass.getSimpleName().toUpperCase());

                try (PreparedStatement statement = connection.prepareStatement(relationshipSQL)) {
                    statement.execute();

                    LOGGER.info("Establishing relationship processed successfully!");
                }
            } else {
                if (!doesEntityExist(clazz)) {
                    throw new SQLException(String.format("Relationship cannot be made! Entity %s doesn't exist in database!", clazz.getSimpleName()));
                } else if (!doesEntityExist(fieldClass)) {
                    var missingEntityName = fieldClass.getSimpleName();

                    throw new SQLException(String.format("Relationship between %s and %s cannot be made! Missing entity %s!", clazz.getSimpleName(), missingEntityName, missingEntityName));
                }
                LOGGER.info("Relationship between entities: {} and {} already exists.", clazz.getSimpleName().toUpperCase(), fieldClass.getSimpleName().toUpperCase());
            }
        }
    }

    void dropEntity(Class<?> clazz) {
        var entityName = ormManagerUtil.getTableName(clazz);

        var dropEntitySQL = "DROP TABLE " + entityName;

        try (PreparedStatement dropEntityStatement = connection.prepareStatement(dropEntitySQL)) {
            dropEntityStatement.execute();
            LOGGER.info("{} entity has been dropped from DB.", entityName);
        } catch (SQLException unknownEntity) {
            LOGGER.info("{} entity doesn't exist in DB.", entityName);
        }
    }

    public boolean doesEntityExist(Class<?> clazz) throws SQLException {
        var searchedEntityName = ormManagerUtil.getTableName(clazz);

        String checkIfEntityExistsSQL = "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE (TABLE_SCHEMA = 'test') AND (TABLE_NAME = '" + searchedEntityName + "');";

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(checkIfEntityExistsSQL);
            resultSet.next();

            return resultSet.getInt(1) == 1;
        }
    }

    public boolean doesRelationshipAlreadyExist(Class<?> clazzToCheck, Class<?> relationToCheck) throws SQLException {
        String findRelationSQL = "SELECT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = '" + ormManagerUtil.getTableName(clazzToCheck) + "';";

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(findRelationSQL);
            resultSet.next();

            while (resultSet.next()) {
                if (resultSet.getString(1).equals(ormManagerUtil.getTableName(relationToCheck))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void persist(Object t) throws SQLException, IllegalAccessException {
        String sqlStatement = ormManagerUtil.getInsertStatement(t);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            ormManagerUtil.mapStatement(t, preparedStatement);
            ormCache.putToCache(t);
        }
    }

    public Object save(Object objectToSave) throws SQLException, IllegalAccessException {
        if (!merge(objectToSave)) {
            String sqlStatement = ormManagerUtil.getInsertStatement(objectToSave);
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS)) {
                ormManagerUtil.mapStatement(objectToSave, preparedStatement);

                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                while (generatedKeys.next()) {
                    for (Field field : ormManagerUtil.getAllDeclaredFieldsFromObject(objectToSave)) {
                        field.setAccessible(true);
                        if (field.isAnnotationPresent(Id.class)) {
                            Long id = generatedKeys.getLong(1);
                            field.set(objectToSave, id);

                            if (OrmManagerUtil.isParent(objectToSave.getClass())) {
                                Objects.requireNonNull(OrmManagerUtil.getChildren(objectToSave))
                                        .forEach(child -> {
                                            try {
                                                Field parentField = OrmManagerUtil.getParent(child);
                                                LOGGER.warn("PARENT FIELD: {}", parentField);
                                                parentField.setAccessible(true);
                                                parentField.set(child, objectToSave);
                                                save(child);
                                            } catch (SQLException | IllegalAccessException e) {
                                                LOGGER.error(e.getMessage(), "When trying to save children with parent at once");
                                            }
                                        });
                            }

                            ormCache.putToCache(objectToSave);
                        }
                    }
                }
            }
        }
        return objectToSave;
    }

    public boolean merge(Object entity) {
        boolean isMerged = false;
        String recordId = ormManagerUtil.getRecordId(entity);

        if (ormCache.isRecordInCache(recordId, entity.getClass()) | isRecordInDataBase(entity)) {
            String queryCheck = String.format("UPDATE %s SET %s WHERE id = ?",
                    ormManagerUtil.getTableClassName(entity),
                    ormManagerUtil.getColumnFieldsWithValuesToString(entity)
            );

            try (PreparedStatement preparedStatement = connection.prepareStatement(queryCheck)) {
                preparedStatement.setString(1, recordId);
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isMerged = preparedStatement.executeUpdate() > 0;
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }

        if (isMerged) {
            ormCache.putToCache(entity);
        }

        return isMerged;
    }

    public boolean delete(Object recordToDelete) {

        boolean isDeleted = false;
        String recordId = "";

        if (isRecordInDataBase(recordToDelete)) {
            String tableName = recordToDelete.getClass().getAnnotation(Table.class).name();
            String queryCheck = String.format("DELETE FROM %s WHERE id = ?", tableName);

            try (PreparedStatement preparedStatement = connection.prepareStatement(queryCheck)) {
                recordId = ormManagerUtil.getRecordId(recordToDelete);
                preparedStatement.setString(1, recordId);
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isDeleted = preparedStatement.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }

            if (isDeleted) {
                OrmManagerUtil.getChildren(recordToDelete).forEach(child -> System.out.println("child to delete : "+child));
                OrmManagerUtil.getChildren(recordToDelete).forEach(child -> ormCache.deleteFromCache(child));
                LOGGER.info("{} (id = {}) has been deleted from DB.", recordToDelete.getClass().getSimpleName(), recordId);
                ormCache.deleteFromCache(recordToDelete);


            }
        }
        return isDeleted;
    }

    public Object update(Object o) throws IllegalAccessException {
        if (OrmManagerUtil.getId(o) != null && isRecordInDataBase(o)) {
            LOGGER.info("This {} has been updated from Data Base.",
                    o.getClass().getSimpleName());
            return findById(OrmManagerUtil.getId(o), o.getClass()).get();
        }
        LOGGER.info("There is no such object with id in database or id of element is null.");
        LOGGER.info("The object {} that was passed to the method was returned.",
                o.getClass().getSimpleName());
        return o;
    }

    public boolean isRecordInDataBase(Object searchedRecord) {
        boolean isInDB = false;

        try {
            isInDB = ormCache.isRecordInCache(OrmManagerUtil.getId(searchedRecord), searchedRecord.getClass());
            if (isInDB) return true;
        } catch (IllegalAccessException e) {
            LOGGER.error("isRecordInDataBase error: " + e.getMessage());
        }

        String tableName = searchedRecord.getClass().getAnnotation(Table.class).name();
        String queryCheck = String.format("SELECT count(*) FROM %s WHERE id = ?", tableName);

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryCheck)) {
            String recordId = ormManagerUtil.getRecordId(searchedRecord);

            preparedStatement.setString(1, recordId);
            LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                isInDB = count == 1;
            }
        } catch (SQLException e) {
            LOGGER.error("isRecordInDataBase error: " + e.getMessage());
        }

        LOGGER.info("This {} {} in Data Base.",
                searchedRecord.getClass().getSimpleName(),
                isInDB ? "exists" : "does not exist");

        return isInDB;
    }

    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        return ormCache.getFromCache(id, cls)
                .or(() -> (loadFromDb(id, cls)));
    }

    private <T1> Optional<T1> loadFromDb(Serializable id, Class<T1> cls) {

        if (id == null) {
            LOGGER.info("Object not found id DB");
            return Optional.empty();
        }

        T1 t = null;
        String sqlStatement = "SELECT * FROM "
                .concat(ormManagerUtil.getTableName(cls))
                .concat(" WHERE id=")
                .concat(id.toString())
                .concat(";");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            t = cls.getDeclaredConstructor().newInstance();

            if (resultSet.next()) {
                ormCache.putToCache(t);
                t = mapperToObject(resultSet, t).orElseThrow();
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            LOGGER.info(String.valueOf(e));
        }

        return Optional.ofNullable(t);
    }

    @SneakyThrows({ReflectiveOperationException.class, SQLException.class})
    public <T> List<T> findAll(Class<T> cls) {

        List<T> allEntities = new ArrayList<>();
        String sqlStatement = "SELECT * FROM " + ormManagerUtil.getTableName(cls);
        LOGGER.info("sqlStatement {}", sqlStatement);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Long id = resultSet.getLong(this.ormManagerUtil.getIdFieldName(cls));
                this.ormCache.getFromCache(id, cls)
                        .ifPresentOrElse(
                                allEntities::add,
                                () -> {
                                    try {
                                        T resultFromDb = cls.getConstructor().newInstance();
                                        ObjectMapper.mapperToObject(resultSet, resultFromDb);
                                        allEntities.add(resultFromDb);
                                        ormCache.putToCache(resultFromDb);
                                    } catch (ReflectiveOperationException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
            }
        }
        return allEntities;
    }

    public <T> Stream<T> findAllAsStream(Class<T> cls) {
        return findAll(cls).stream();
        // todo: try to do it lazily, instantiating the objects on demand one by one
    }

    public <T> Iterable<T> findAllAsIterable(Class<T> cls) {
        return findAll(cls);
        // todo there are problems with resource leakage because we cannot close the resultSet
//        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
//        LOGGER.info("sqlStatement {}", sqlStatement);
//        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
//            ResultSet resultSet = preparedStatement.executeQuery();
//            return () -> new Iterator<Object>() {
//                @Override
//                public boolean hasNext() {
//                    try {
//                        return resultSet.next();
//                    } catch (SQLException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//
//                @Override
//                public Object next() {
//                    if (!hasNext()) throw new NoSuchElementException();
//                    Object t = null;
//                    try {
//                        t = cls.getConstructor().newInstance();
//                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
//                             NoSuchMethodException e) {
//                        LOGGER.info(String.valueOf(e));
//                    }
//                    ObjectMapper.mapperToObject(resultSet, t);
//                    return t;
//                }
//            };
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
    }
}
