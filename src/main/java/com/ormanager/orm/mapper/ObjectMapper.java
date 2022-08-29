package com.ormanager.orm.mapper;

import com.ormanager.orm.annotation.Column;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ObjectMapper {
    public static <T> Optional<T> mapperToObject(ResultSet resultSet, T t) {
        try {
            for (Field field : t.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Column.class)) {
                    if (field.getType() == Integer.class) {
                        field.set(t, resultSet.getInt(field.getAnnotation(Column.class).name()));
                    } else if (field.getType() == Long.class) {
                        field.set(t, resultSet.getLong(field.getAnnotation(Column.class).name()));
                    } else if (field.getType() == String.class) {
                        field.set(t, resultSet.getString(field.getAnnotation(Column.class).name()));
                    } else if (field.getType() == LocalDate.class) {
                        field.set(t, resultSet.getDate(field.getAnnotation(Column.class).name()).toLocalDate());
                    }
                } else {
                    if (field.getType() == Integer.class) {
                        field.set(t, resultSet.getInt(field.getName()));
                    } else if (field.getType() == Long.class) {
                        field.set(t, resultSet.getLong(field.getName()));
                    } else if (field.getType() == String.class) {
                        field.set(t, resultSet.getString(field.getName()));
                    } else if (field.getType() == LocalDate.class) {
                        field.set(t, resultSet.getDate(field.getName()).toLocalDate());
                    }
                }
            }
        } catch (IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(t);
    }
}