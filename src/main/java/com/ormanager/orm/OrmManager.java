package com.ormanager.orm;

import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrmManager<T> {
    private Connection con;
    private AtomicLong id = new AtomicLong(0L);
    private int idIndex = 1;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "bsyso", "root");
    }

    public void persist(T t) {
        var length = getAllDeclaredFieldsFromObject(t).size() - 1;
        var questionMarks = IntStream.range(0, length)
                .mapToObj(q -> "?")
                .collect(Collectors.joining(","));

        String sqlStatement = "INSERT INTO "
                .concat(getTableClassName(t))
                .concat("(")
                //.concat(getIdName(t))
                //.concat(",")
                .concat(getAllValuesFromListToString(t))
                .concat(") VALUES(")
                .concat(questionMarks)
                .concat(");");

        try (Connection connection = getConnection().con;
             PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            for (Field field : getAllColumns(t)) {
                field.setAccessible(true);

                var index = getAllColumns(t).indexOf(field) + 1;

                if (field.getType() == String.class) {
                    preparedStatement.setString(index, (String) field.get(t));
                } else if (field.getType() == LocalDate.class) {
                    Date date = Date.valueOf((LocalDate) field.get(t));
                    preparedStatement.setDate(index, date);
                }
            }
            preparedStatement.executeUpdate();
        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public T save(T t) {
        persist(t);

        return t;
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

    public List<Field> getAllColumns(T t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }
}
