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
final class OrmManagerUtil {

    void setObjectToNull(Object targetObject) {
        Arrays.stream(targetObject.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            try {
                field.set(targetObject, null);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage());
            }
        });
    }

    static Serializable getId(Object o) throws IllegalAccessException {

        Optional<Field> optionalId = getIdField(o);
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            return (Serializable) optionalId.get().get(o);
        }
        return null;
    }

    static Optional<Field> getIdField(Object o) {
        return Arrays.stream(o.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
    }

    String getRecordId(Object recordInDb) {
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

    String getTableClassName(Object t) {
        return t.getClass().getAnnotation(Table.class).name();
    }

    List<Field> getAllDeclaredFieldsFromObject(Object t) {
        return Arrays.asList(t.getClass().getDeclaredFields());
    }

    String getAllValuesFromListToString(Object t) {
        return String.join(",", getAllValuesFromObject(t));
    }

    List<String> getAllValuesFromObject(Object t) {
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
            }
        }
        return strings;
    }

    String getSqlTypeForField(Field field) {
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

    String getTableName(Class<?> clazz) {
        var tableAnnotation = Optional.ofNullable(clazz.getAnnotation(Table.class));

        return tableAnnotation.isPresent() ? tableAnnotation.get().name() : clazz.getSimpleName().toLowerCase();
    }

    String getColumnFieldsWithValuesToString(Object t) {
        try {
            return String.join(", ", getColumnFieldsWithValues(t));
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    List<Field> getBasicFieldsFromClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .filter(field -> !field.isAnnotationPresent(ManyToOne.class))
                .filter(field -> field.getType() != Collection.class)
                .toList();
    }

    String getIdFieldName(Class<?> clazz) throws NoSuchFieldException {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny()
                .orElseThrow(() -> new NoSuchFieldException(String.format("ID field not found in class %s !", clazz)))
                .getName();
    }

    List<String> getColumnFieldsWithValues(Object t) throws IllegalAccessException {
        List<String> strings = new ArrayList<>();

        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            field.setAccessible(true);
            //TODO CLEAN THE MESS
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

    List<Field> getAllColumnsButId(Object t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    Long getAllColumnsButIdAndOneToMany(Object t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .filter(v -> !v.isAnnotationPresent(OneToMany.class))
                .count();
    }

    void mapStatement(Object t, PreparedStatement preparedStatement) throws SQLException, IllegalAccessException {
        for (Field field : getAllColumnsButId(t)) {
            field.setAccessible(true);
            var index = getAllColumnsButId(t).indexOf(field) + 1;
            if (field.getType() == String.class) {
                preparedStatement.setString(index, (String) field.get(t));
            } else if (field.getType() == LocalDate.class) {
                Date date = Date.valueOf((LocalDate) field.get(t));
                preparedStatement.setDate(index, date);
            } else if (field.getType() == Long.class) {
                preparedStatement.setLong(index, (Long) field.get(t));
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                Field[] innerFields = field.getType().getDeclaredFields();
                for (Field fieldInPublisher : innerFields) {
                    fieldInPublisher.setAccessible(true);
                    if (fieldInPublisher.isAnnotationPresent(Id.class) && fieldInPublisher.getType() == Long.class) {
                        if (field.get(t) != null) {
                            preparedStatement.setLong(index, (Long) fieldInPublisher.get(field.get(t)));
                        }
                    }
                }
            } else if (!field.isAnnotationPresent(OneToMany.class)) {
                preparedStatement.setObject(index, null);
            }
        }

        LOGGER.info("PREPARED STATEMENT : {}", preparedStatement);
        preparedStatement.executeUpdate();
    }

    String getInsertStatement(Object t) {
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
}
