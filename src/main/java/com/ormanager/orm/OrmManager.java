package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.OneToMany;
import com.ormanager.orm.annotation.Table;
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
import java.util.List;
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

        var columnNamesLength = columnNames.length();

        String sql = "CREATE TABLE IF NOT EXISTS " + getTableName(clazz) + " (" + id.getName() + " UNSIGNED AUTO_INCREMENT PRIMARY KEY, " + columnNames.substring(0, columnNamesLength - 2) + ")";

        LOGGER.info("CREATE TABLE SQL statement is being prepared now: " + sql);

        processOneToManyRelationship(clazz);

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

    private void processOneToManyRelationship(Class<?> clazz) throws DataConnectionException {
        var fieldCounter = 0;

        for (var field : getRelationshipFieldsIfExist(clazz, OneToMany.class)) {
            var foreignKey = field.getAnnotation(OneToMany.class).mappedBy();

            System.out.println("FOREIGN KEY: " + foreignKey);

            var fieldName = field.getName();
            System.out.println("FIELD NAME: " + fieldName);

            var genericType = (ParameterizedType) field.getGenericType();
            System.out.println("GENERIC TYPE: " + genericType);

            Type[] types = genericType.getActualTypeArguments();
            System.out.println("TYPES: " + Arrays.toString(types));

            var relatedClassType = (Class<?>) types[fieldCounter];
            System.out.println("RELATED CLASS TYPE: " + relatedClassType);

            try {
                var foreignKeyClassType = relatedClassType.getDeclaredField(foreignKey).getType();
                System.out.println("FOREIGN KEY CLASS TYPE: " + foreignKeyClassType);
            }
            catch (Exception e) {
                throw new DataConnectionException(String.format("Error occurred when trying to get foreign key's type. The foreign key of %s does not exist on %s", foreignKey, relatedClassType));
            }
        }
    }

    public static void main(String[] args) throws SQLException, DataConnectionException {
        OrmManager.getConnection().register(Publisher.class);
    }
}
