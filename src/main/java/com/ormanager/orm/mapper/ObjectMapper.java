package com.ormanager.orm.mapper;

import com.ormanager.orm.annotation.Column;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ObjectMapper {
    private ObjectMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> Optional<T> mapperToObject(ResultSet resultSet, T t) {
        try {
            for (Field field : t.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = "";
                if (field.isAnnotationPresent(Column.class) && !field.getAnnotation(Column.class).name().equals("")) {
                    fieldName = field.getAnnotation(Column.class).name();
                } else {
                    fieldName = field.getName();
                }
                if (field.getType() == Integer.class) {
                    field.set(t, resultSet.getInt(fieldName));
                } else if (field.getType() == Long.class) {
                    field.set(t, resultSet.getLong(fieldName));
                } else if (field.getType() == String.class) {
                    field.set(t, resultSet.getString(fieldName));
                } else if (field.getType() == LocalDate.class) {
                    field.set(t, resultSet.getDate(fieldName).toLocalDate());
                }
            }
        } catch (IllegalAccessException | SQLException e) {
            LOGGER.warn(e.getMessage());
        }
        return Optional.of(t);
    }

    public static <T> List<T> mapperToList(ResultSet resultSet, Class<?> cls) {
        List<T> list = new ArrayList<>();
        T t;
        try {
            t = (T) cls.getDeclaredConstructor().newInstance();
            while (resultSet.next()) {
                t = mapperToObject(resultSet, t).orElseThrow();
                list.add(t);
            }
        } catch (SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.warn(e.getMessage());
        }
        return list;
    }
}
