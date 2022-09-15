package com.ormanager.orm;

import com.ormanager.SchemaOperationType;
import com.ormanager.jdbc.ConnectionToDB;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.Table;
import com.ormanager.orm.exception.IdAlreadySetException;
import com.ormanager.orm.mapper.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.ormanager.orm.OrmManagerUtil.*;
import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;
import static java.util.Objects.requireNonNull;

@Slf4j(topic = "OrmManager")
public class OrmManager implements IOrmManager {
    private final Cache ormCache;
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
        this.connection = connection;
        ormCache = new Cache();
    }

    private OrmManager(String url, String username, String password) throws SQLException {
        this.connection = DriverManager.
                getConnection(url, username, password);
        ormCache = new Cache();
    }

    public Cache getOrmCache() {
        return ormCache;
    }

    public void register(Class<?>... entityClasses) throws SQLException, NoSuchFieldException {
        for (var clazz : entityClasses) {
            register(clazz);
        }
    }

    public void register(Class<?> clazz) throws SQLException, NoSuchFieldException {
        if (doesEntityExist(clazz)) {
            LOGGER.info("{} already exists in database!", clazz.getSimpleName());
            return;
        }

        var tableName = OrmManagerUtil.getTableName(clazz);

        var idFieldName = OrmManagerUtil.getIdFieldName(clazz);

        var idFieldType = OrmManagerUtil.getIdField(clazz);

        var idSqlType = OrmManagerUtil.getSqlIdTypeForFieldForGivenOperation(SchemaOperationType.REGISTER_ENTITY, idFieldType);

        var basicFields = OrmManagerUtil.getBasicFieldsFromClass(clazz);

        var fieldsAndTypes = new StringBuilder();

        for (var basicField : basicFields) {
            var sqlTypeForField = OrmManagerUtil.getSqlTypeForField(basicField);

            if (basicField.isAnnotationPresent(Column.class) && !basicField.getAnnotation(Column.class).name().equals("")) {
                fieldsAndTypes.append(" ").append(basicField.getAnnotation(Column.class).name());
            } else {
                fieldsAndTypes.append(" ").append(basicField.getName());
            }
            fieldsAndTypes.append(sqlTypeForField);
        }

        StringBuilder registerSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (" + idFieldName + idSqlType
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
            var fieldTableAnnotationClassName = OrmManagerUtil.getTableName(fieldClass);
            var fieldNameFromManyToOneAnnotation = field.getAnnotation(ManyToOne.class).columnName();
            var fieldName = fieldNameFromManyToOneAnnotation.equals("") ? fieldClass.getSimpleName().toLowerCase() + "_id" : fieldNameFromManyToOneAnnotation;
            var fieldClassIdName = OrmManagerUtil.getIdFieldName(fieldClass);
            var clazzTableName = OrmManagerUtil.getTableName(clazz);
            var fieldIdType = OrmManagerUtil.getIdField(fieldClass);
            var fieldIdSqlType = OrmManagerUtil.getSqlIdTypeForFieldForGivenOperation(SchemaOperationType.CREATE_RELATION_FOR_ENTITY, fieldIdType);

            if (doesEntityExist(clazz) && doesEntityExist(fieldClass) && !(doesRelationshipAlreadyExist(clazz, fieldClass))) {

                var relationshipSQL = "ALTER TABLE " + clazzTableName + " ADD COLUMN " + fieldName + fieldIdSqlType +
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

    public void dropEntity(Class<?> clazz) {
        var entityName = OrmManagerUtil.getTableName(clazz);

        var dropEntitySQL = "DROP TABLE " + entityName;

        try (PreparedStatement dropEntityStatement = connection.prepareStatement(dropEntitySQL)) {
            dropEntityStatement.execute();
            LOGGER.info("{} entity has been dropped from DB.", entityName);
        } catch (SQLException unknownEntity) {
            LOGGER.info("{} entity doesn't exist in DB.", entityName);
        }
    }

    public boolean doesEntityExist(Class<?> clazz) throws SQLException {
        var searchedEntityName = OrmManagerUtil.getTableName(clazz);

        String checkIfEntityExistsSQL = "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE (TABLE_SCHEMA = 'test') AND (TABLE_NAME = '" + searchedEntityName + "');";

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(checkIfEntityExistsSQL);
            resultSet.next();

            return resultSet.getInt(1) == 1;
        }
    }

    public boolean doesRelationshipAlreadyExist(Class<?> clazzToCheck, Class<?> relationToCheck) throws SQLException {
        String findRelationSQL = "SELECT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = '" + OrmManagerUtil.getTableName(clazzToCheck) + "';";

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(findRelationSQL);
            resultSet.next();

            while (resultSet.next()) {
                if (resultSet.getString(1) != null && resultSet.getString(1).equals(OrmManagerUtil.getTableName(relationToCheck))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void persist(Object objectToPersist) throws SQLException, IllegalAccessException {
        String sqlStatement = getInsertStatement(objectToPersist);
        Field field = getIdField(objectToPersist).get();
        field.setAccessible(true);

        if (field.get(objectToPersist) != null
                && getIdField(objectToPersist).orElseThrow().getType() != String.class) {
            throw new IdAlreadySetException("Id was set already");
        }

        generateUuidForProperObject(objectToPersist);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            mapStatement(objectToPersist, preparedStatement);
            getChildrenAndSaveThem(objectToPersist, objectToPersist.getClass());
            ormCache.putToCache(objectToPersist);
        }
    }

    @SneakyThrows
    public Object save(Object objectToSave) {

        Class<?> objectClass = objectToSave.getClass();

        if (!merge(objectToSave)) {
            String sqlStatement = OrmManagerUtil.getInsertStatement(objectToSave);

            generateUuidForProperObject(objectToSave);

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS)) {
                mapStatement(objectToSave, preparedStatement);
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

                if (OrmManagerUtil.getIdField(objectClass).getType() != UUID.class) {
                    while (generatedKeys.next()) {
                        for (Field field : getAllDeclaredFieldsFromObject(objectToSave)) {
                            field.setAccessible(true);
                            if (field.isAnnotationPresent(Id.class)) {
                                var id = generatedKeys.getLong(1);
                                field.set(objectToSave, id);
                            }
                        }
                    }
                }
                getChildrenAndSaveThem(objectToSave, objectClass);
            }
        }
        return objectToSave;
    }

    public boolean merge(Object entity) {
        boolean isMerged = false;
        String recordId = OrmManagerUtil.getRecordId(entity);
        Class<?> recordClass = entity.getClass();

        if (ormCache.isRecordInCache(recordId, recordClass) | isRecordInDataBase(entity)) {
            String queryCheck = String.format("UPDATE %s SET %s WHERE id = ?",
                    OrmManagerUtil.getTableClassName(entity),
                    OrmManagerUtil.getColumnFieldsWithValuesToString(entity)
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
            getChildrenAndSaveThem(entity, recordClass);
        }

        return isMerged;
    }

    private void getChildrenAndSaveThem(Object objectToSave, Class<?> objectClass) {
        if (OrmManagerUtil.isParent(objectClass)) {
            requireNonNull(OrmManagerUtil.getChildren(objectToSave))
                    .forEach(child -> {
                        try {
                            Field parentField = OrmManagerUtil.getParent(child);
                            LOGGER.info("PARENT FIELD: {}", parentField);
                            parentField.setAccessible(true);
                            parentField.set(child, objectToSave);
                            save(child);
                        } catch (IllegalAccessException e) {
                            LOGGER.error(e.getMessage(), "When trying to save child while merging or saving parent.");
                        }
                    });
        }

        ormCache.putToCache(objectToSave);
    }

    public boolean delete(Object recordToDelete) {

        boolean isDeleted = false;
        Class<?> recordToDeleteClass = recordToDelete.getClass();
        String recordId = "";

        if (isRecordInDataBase(recordToDelete)) {
            String tableName = getTableClassName(recordToDelete);
            String queryCheck = String.format("DELETE FROM %s WHERE id = ?", tableName);

            try (PreparedStatement preparedStatement = connection.prepareStatement(queryCheck)) {
                recordId = getRecordId(recordToDelete);
                preparedStatement.setString(1, recordId);
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isDeleted = preparedStatement.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }

            if (isDeleted) {
                deleteChildren(recordToDelete);

                LOGGER.info("{} (id = {}) has been deleted from DB.", recordToDeleteClass.getSimpleName(), recordId);
                ormCache.deleteFromCache(recordToDelete);
            }
        }
        return isDeleted;
    }

    private void deleteChildren(Object parent) {
        if (isParent(parent.getClass())) {
            requireNonNull(getChildren(parent))
                    .forEach(child -> {
                        LOGGER.info("Child to delete: {}", child);
                        ormCache.deleteFromCache(child);
                    });
        }
    }

    public List<Object> getChildrenFromDataBase(Field childrenField, Object obj, Class<?> clazz) {
        Object ch = null;
        List<Object> children = new ArrayList<>();

        String sqlStatement = "SELECT * FROM "
                .concat(childrenField.getName())
                .concat(" WHERE ")
                .concat(obj.getClass().getSimpleName().toLowerCase())
                .concat("_id")
                .concat(" = ")
                .concat(OrmManagerUtil.getId(obj).toString())
                .concat(";");

        try (PreparedStatement preparedStatement1 = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet1 = preparedStatement1.executeQuery();
            ch = clazz.getDeclaredConstructor().newInstance();
            if (resultSet1.next()) {
                ch = mapperToObject(resultSet1, ch).orElseThrow();
                children.add(ch);

            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return children;
    }

    public Serializable getParentIdFromDatabase(String parentName, String objectName, String objectId) throws SQLException {
        String sqlStatement = "SELECT "
                .concat(parentName)
                .concat("_id")
                .concat(" FROM ")
                .concat(objectName)
                .concat(" WHERE ")
                .concat("id = ")
                .concat(objectId)
                .concat(";");

        try (PreparedStatement preparedStatement1 = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet1 = preparedStatement1.executeQuery();

            if (resultSet1.next()) {
                return resultSet1.getLong(1);
            }
        }
        return null;
    }

    public Object getParentFromDatabase(String parentName, Object obj, Class<?> clazz) throws SQLException {
        Object parent = null;

        Table table = obj.getClass().getAnnotation(Table.class);
        String tableName = table.name();

        String sqlStatement = "SELECT * FROM "
                .concat(parentName)
                .concat(" WHERE ")
                .concat("id = ")
                .concat(String.valueOf(getParentIdFromDatabase(clazz.getSimpleName().toLowerCase(), tableName, OrmManagerUtil.getId(obj).toString())))
                .concat(";");

        try (PreparedStatement preparedStatement1 = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet1 = preparedStatement1.executeQuery();
            parent = clazz.getDeclaredConstructor().newInstance();
            if (resultSet1.next()) {
                parent = mapperToObject(resultSet1, parent).orElseThrow();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return parent;
    }


    public Object update(Object obj) {
        if (OrmManagerUtil.getId(obj) != null && isRecordInDataBase(obj)) {
            LOGGER.info("This {} has been updated from Data Base.",
                    obj.getClass().getSimpleName());

            Object t = null;
            Object parent = null;
            List<Object> children = null;
            Class<?> classType;

            String sqlStatement = "SELECT * FROM "
                    .concat(OrmManagerUtil.getTableName(obj.getClass()))
                    .concat(" WHERE id='")
                    .concat(OrmManagerUtil.getId(obj).toString())
                    .concat("';");

            if (isParent(obj.getClass())) {
                Field field = getChild(obj);
                classType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                children = getChildrenFromDataBase(field, obj, classType);
                children.forEach(this::update);
            }

            if (isChild(obj.getClass())) {
                Field field = getParent(obj);
                Table table = field.getType().getAnnotation(Table.class);
                String tableName = table.name();

                try {
                    parent = getParentFromDatabase(tableName, obj, field.getType());
                } catch (Exception e) {
                    LOGGER.info(String.valueOf(e));
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
                ResultSet resultSet = preparedStatement.executeQuery();
                t = obj.getClass().getDeclaredConstructor().newInstance();

                if (resultSet.next()) {

                    t = mapperToObject(resultSet, t).orElseThrow();
                    ormCache.deleteFromCache(ormCache.getFromCache(OrmManagerUtil.getId(obj), obj.getClass()).get());

                    if (children != null) {

                        Field child1 = getChild(t);
                        child1.setAccessible(true);
                        child1.set(t, children);
                        ormCache.putToCache(t);

                        for (Object child2 : children) {
                            try {
                                Field temp = getParent(child2);
                                temp.setAccessible(true);
                                temp.set(child2, t);
                                ormCache.putToCache(child2);

                            } catch (IllegalAccessException e) {
                                LOGGER.warn(e.getMessage());
                            }
                        }

                        return t;
                    }

                    if (parent != null) {
                        Field temp = getParent(obj);
                        temp.setAccessible(true);
                        temp.set(t, parent);
                        ormCache.putToCache(t);
                        return t;
                    }

                    ormCache.putToCache(t);

                }
            } catch (Exception e) {
                LOGGER.info(String.valueOf(e));
            }

            return t;
        }
        LOGGER.info("There is no such object with id in database or id of element is null.");
        LOGGER.info("The object {} that was passed to the method was returned.",
                obj.getClass().getSimpleName());
        return obj;
    }

    public boolean isRecordInDataBase(Object searchedRecord) {

        boolean isInDB = ormCache.isRecordInCache(OrmManagerUtil.getId(searchedRecord), searchedRecord.getClass());
        if (isInDB) return true;


        String tableName = searchedRecord.getClass().getAnnotation(Table.class).name();
        String queryCheck = String.format("SELECT count(*) FROM %s WHERE id = ?", tableName);

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryCheck)) {
            String recordId = OrmManagerUtil.getRecordId(searchedRecord);

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
        if (id == null || cls == null) throw new NoSuchElementException();

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
                .concat(OrmManagerUtil.getTableName(cls))
                .concat(" WHERE id='")
                .concat(id.toString())
                .concat("';");

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            t = cls.getDeclaredConstructor().newInstance();

            if (resultSet.next()) {
                t = mapperToObject(resultSet, t).orElseThrow();
                ormCache.putToCache(t);
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
        String sqlStatement = "SELECT * FROM " + OrmManagerUtil.getTableName(cls);
        LOGGER.info("sqlStatement {}", sqlStatement);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                var id = OrmManagerUtil.isIdFieldNumericType(cls) ? resultSet.getLong(OrmManagerUtil.getIdFieldName(cls))
                        : UUID.fromString(resultSet.getString(OrmManagerUtil.getIdFieldName(cls)));
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

    public <T> Stream<T> findAllAsStream(Class<T> cls) throws SQLException {
        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
        LOGGER.info("sqlStatement {}", sqlStatement);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        ResultSet resultSet = preparedStatement.executeQuery();

        return StreamSupport.stream(new OrmSpliterator<T>(resultSet, cls, ormCache), false);
    }

    public <T> IterableORM<T> findAllAsIterable(Class<T> cls) throws SQLException {
        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
        LOGGER.info("sqlStatement {}", sqlStatement);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        ResultSet resultSet = preparedStatement.executeQuery();

        return new IterableORM<T>() {
            @Override
            public boolean hasNext() {
                try {
                    var result = resultSet.next();
                    if (!result) {
                        close();
                    }
                    return result;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @SneakyThrows
            @Override
            public T next() {
                var id = OrmManagerUtil.isIdFieldNumericType(cls) ? resultSet.getLong(OrmManagerUtil.getIdFieldName(cls))
                        : UUID.fromString(resultSet.getString(OrmManagerUtil.getIdFieldName(cls)));
                try {
                    id = resultSet.getObject(OrmManagerUtil.getIdFieldName(cls)).toString();
                } catch (SQLException | NoSuchFieldException e) {
                    LOGGER.warn(e.getMessage());
                }
                return (ormCache.getFromCache(id, cls)
                        .or(() -> {
                                    T resultFromDb = null;
                                    try {
                                        resultFromDb = cls.getConstructor().newInstance();
                                        ObjectMapper.mapperToObject(resultSet, resultFromDb);
                                        ormCache.putToCache(resultFromDb);

                                    } catch (ReflectiveOperationException e) {
                                        LOGGER.warn(e.getMessage());
                                    }
                                    return Optional.ofNullable(resultFromDb);
                                }
                        )).get();
            }

            @Override
            public void close() {
                try {
                    resultSet.close();
                    LOGGER.info("ResultSet closed");
                } catch (SQLException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        };
    }
}