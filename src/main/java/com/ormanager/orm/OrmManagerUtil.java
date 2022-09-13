package com.ormanager.orm;

import com.ormanager.orm.annotation.*;
import com.ormanager.orm.exception.OrmFieldTypeException;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public final class OrmManagerUtil {

    private OrmManagerUtil() {
        throw new IllegalStateException("Utility class");
    }

    static Serializable getId(Object o) {

        Optional<Field> optionalId = getIdField(o);

        if (optionalId.isEmpty()) return null;

        optionalId.get().setAccessible(true);

        try {
            return (Serializable) optionalId.get().get(o);
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage(), "When trying to get Serializable ID.");
            return null;
        }
    }

    static Optional<Field> getIdField(Object o) {
        return Arrays.stream(o.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
    }

    static Field getIdField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new OrmFieldTypeException("ID field not found!"));
    }

    static String getSqlIdRegisterStatementFromField(Field field) {
        var fieldType = field.getType();

        if (fieldType == long.class || fieldType == Long.class) {
            return " BIGINT UNSIGNED AUTO_INCREMENT,";
        } else if (fieldType == UUID.class) {
            return " VARCHAR(36),";
        }
        throw new OrmFieldTypeException("Could not get sql type for given field: " + fieldType);
    }

    static String getSqlIdCreateRelationStatementFromField(Field field) {
        var fieldType = field.getType();

        if (fieldType == long.class || fieldType == Long.class) {
            return " BIGINT UNSIGNED,";
        } else if (fieldType == UUID.class) {
            return " VARCHAR(36),";
        }
        throw new OrmFieldTypeException("Could not get sql type for given field: " + fieldType);
    }

    static String getRecordId(Object recordInDb) {
        if (recordInDb == null) return null;

        Optional<Field> optionalId = getIdField(recordInDb);
        if (optionalId.isEmpty()) return null;

        optionalId.get().setAccessible(true);
        Object record = null;
        try {
            record = optionalId.get().get(recordInDb);
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage(), "When getting record ID to String");
        }
        return record != null ? record.toString() : null;
    }

    static boolean doesClassHaveGivenRelationship(Class<?> clazz, Class<? extends Annotation> relationAnnotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(relationAnnotation));
    }

    static List<Field> getRelationshipFields(Class<?> clazz, Class<? extends Annotation> relationAnnotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(relationAnnotation))
                .toList();
    }

    static String getTableClassName(Object t) {
        return t.getClass().getAnnotation(Table.class).name();
    }

    static List<Field> getAllDeclaredFieldsFromObject(Object t) {
        return Arrays.asList(t.getClass().getDeclaredFields());
    }

    static String getAllValuesFromListToString(Object t) {
        return String.join(",", getAllValuesFromObject(t));
    }

    static List<String> getAllValuesFromObject(Object t) {
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
            } else if (!Collection.class.isAssignableFrom(field.getType())
                    && !field.isAnnotationPresent(Id.class)) {
                strings.add(field.getName());
            } else if (field.isAnnotationPresent(Id.class) && !isIdFieldNumericType(field)) {
                strings.add(field.getName());
            }
        }
        return strings;
    }

    static String getSqlTypeForField(Field field) {
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

    static String getTableName(Class<?> clazz) {
        var tableAnnotation = Optional.ofNullable(clazz.getAnnotation(Table.class));

        return tableAnnotation.isPresent() ? tableAnnotation.get().name() : clazz.getSimpleName().toLowerCase();
    }

    static String getColumnFieldsWithValuesToString(Object t) {
        try {
            return String.join(", ", getColumnFieldsWithValues(t));
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    static List<Field> getBasicFieldsFromClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .filter(field -> !field.isAnnotationPresent(ManyToOne.class))
                .filter(field -> field.getType() != Collection.class)
                .toList();
    }

    static String getIdFieldName(Class<?> clazz) throws NoSuchFieldException {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new NoSuchFieldException(String.format("ID field not found in class %s !", clazz)))
                .getName();
    }

    static List<String> getColumnFieldsWithValues(Object t) throws IllegalAccessException {
        List<String> strings = new ArrayList<>();

        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Column.class)) {
                if (!Objects.equals(field.getDeclaredAnnotation(Column.class).name(), "")) {
                    strings.add(field.getDeclaredAnnotation(Column.class).name() + "='" + field.get(t) + "'");
                } else {
                    strings.add(field.getName() + "='" + field.get(t) + "'");
                }
            } else if (field.isAnnotationPresent(ManyToOne.class) && field.get(t) != null) {
                if (getRecordId(field.get(t)) != null) {
                    String recordId = getRecordId(field.get(t));
                    strings.add(field.getDeclaredAnnotation(ManyToOne.class).columnName() + "='" + recordId + "'");
                }
            } else if (!Collection.class.isAssignableFrom(field.getType())
                    && !field.isAnnotationPresent(Id.class)
                    && !field.isAnnotationPresent(ManyToOne.class)) {
                strings.add(field.getName() + "='" + field.get(t) + "'");
            }
        }
        return strings;
    }

    static List<Field> getAllColumnsButId(Object t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    static Long getAllColumnsButIdAndOneToMany(Object t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .filter(v -> !v.isAnnotationPresent(OneToMany.class))
                .count();
    }

    static Long getAllColumnsButOneToMany(Object t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(OneToMany.class))
                .count();
    }

    static void mapStatement(Object t, PreparedStatement preparedStatement) throws SQLException, IllegalAccessException {
        var objectIdField = getIdField(t).orElseThrow(() -> new OrmFieldTypeException("No ID field found!"));
        var objectFields = objectIdField.getType() == UUID.class ? getAllDeclaredFieldsFromObject(t) : getAllColumnsButId(t);

        var index = 0;
        for (Field field : objectFields) {
            field.setAccessible(true);
            index = objectFields.indexOf(field) + 1;
            if (field.getType() == String.class) {
                preparedStatement.setString(index, (String) field.get(t));
            } else if (field.getType() == LocalDate.class) {
                Date date = Date.valueOf((LocalDate) field.get(t));
                preparedStatement.setDate(index, date);
            } else if (field.getType() == Long.class) {
                preparedStatement.setLong(index, (Long) field.get(t));
            } else if (field.isAnnotationPresent(Id.class) && !isIdFieldNumericType(field)) {
                preparedStatement.setString(index, field.get(t).toString());
            } else if (!field.isAnnotationPresent(OneToMany.class)) {
                preparedStatement.setObject(index, null);
            }
        }
        setParentFieldOfChildForStatement(t, preparedStatement, index);

        LOGGER.info("PREPARED STATEMENT : {}", preparedStatement);
        preparedStatement.executeUpdate();
    }

    static String getInsertStatement(Object t) {
        var length = getAllColumnsLengthForInsertStatement(t);

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
        return sqlStatement;
    }

    private static int getAllColumnsLengthForInsertStatement(Object t) {
        var objectIdFieldType = getIdField(t.getClass());

        return isIdFieldNumericType(objectIdFieldType) ? getAllColumnsButIdAndOneToMany(t).intValue() : getAllColumnsButOneToMany(t).intValue();
    }

    static boolean isParent(Class<?> keyClazz) {
        return doesClassHaveGivenRelationship(keyClazz, OneToMany.class);
    }

    static boolean isChild(Class<?> keyClazz) {
        return doesClassHaveGivenRelationship(keyClazz, ManyToOne.class);
    }

    static List<Object> getChildren(Object parent) {
        Optional<Field> children = Arrays.stream(parent.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OneToMany.class))
                .findFirst();

        if (children.isEmpty()) return null;
        Field childrenField = children.get();

        if (!Collection.class.isAssignableFrom(childrenField.getType())) return null;

        childrenField.setAccessible(true);

        Object object = null;
        try {
            object = childrenField.get(parent);
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage(), "When trying to get children from parent that are not in cache");
        }
        assert object != null;
        return new ArrayList<>((Collection<?>) object);
    }

    static Field getParent(Object childObject) {

        Optional<Field> parent = Arrays.stream(childObject.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ManyToOne.class))
                .findFirst();
        if (parent.isEmpty()) return null;
        return parent.get();
    }

    private static void setParentFieldOfChildForStatement(Object o, PreparedStatement preparedStatement, int index) {
        var objectManyToOneFields = getRelationshipFields(o.getClass(), ManyToOne.class);

        for (var manyToOneField : objectManyToOneFields) {
            manyToOneField.setAccessible(true);
            Field[] parentFields = manyToOneField.getType().getDeclaredFields();

            Arrays.stream(parentFields)
                    .filter(field -> field.isAnnotationPresent(Id.class))
                    .peek(field -> field.setAccessible(true))
                    .forEach(field -> {
                       for (var objectManyToOneField : objectManyToOneFields) {
                           try {
                               if (objectManyToOneField.get(o) != null && isIdFieldNumericType(field)) {
                                   preparedStatement.setObject(index, field.get(objectManyToOneField.get(o)));
                               } else if (objectManyToOneField.get(o) != null) {
                                   preparedStatement.setString(index, field.get(objectManyToOneField.get(o)).toString());
                               } else {
                                   preparedStatement.setObject(index, null);
                               }
                           } catch (IllegalAccessException | SQLException e) {
                               throw new RuntimeException(e);
                           }
                       }
                    });
        }
    }

    static boolean isIdFieldNumericType(Field field) {
        return Number.class.isAssignableFrom(field.getType());
    }

    static boolean isIdFieldNumericType(Class<?> cls) {
        return Number.class.isAssignableFrom( getIdField(cls).getType());
    }
}