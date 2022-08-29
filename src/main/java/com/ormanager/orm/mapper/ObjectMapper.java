package com.ormanager.orm.mapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ObjectMapper {
    public static <T> Optional<T> mapperToObject(ResultSet resultSet, T t) {
        try {
            for (Field field : t.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (resultSet.getObject(field.getName()).equals("com.ormanager.client.entity.Book.id")) {
                    field.set(t, ((Integer) resultSet.getObject(field.getName())).longValue());
                }
                field.set(t, resultSet.getObject(field.getName()));
            }
        } catch (IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(t);
    }
}
