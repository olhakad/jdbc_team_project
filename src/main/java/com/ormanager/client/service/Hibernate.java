package com.ormanager.client.service;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ormanager.orm.annotation.*;

public class Hibernate<T> {
    private Connection con;
    private AtomicLong id = new AtomicLong(0L);
    private int idIndex = 1;

    public static <T> Hibernate<T> getConnection() throws SQLException {
        return new Hibernate<T>();
    }

    private Hibernate() throws SQLException {
        this.con = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
    }

    public void persist(T t) throws IllegalArgumentException, SQLException, IllegalAccessException {
        Class<?> clazz = t.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();
        Field pk = null;
        ArrayList<Field> columns = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(",");
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class)) {
                pk = field;
            } else {
                joiner.add(field.getName());
                columns.add(field);
            }
        }
        int length = columns.size() + 1;
        String qMarks = IntStream.range(0, length)
                .mapToObj(e -> "?")
                .collect(Collectors.joining(","));
        String sql = "INSERT INTO " + clazz.getSimpleName() + "( " + pk.getName() + "," + joiner.toString() + ") " + "values (" + qMarks + ")";
        System.out.println(sql);
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        if (pk.getType() == Long.class) {
            preparedStatement.setLong(idIndex, id.incrementAndGet());
        }
        idIndex++;
        for (Field field : columns) {
            field.setAccessible(true);
            if (field.getType() == String.class) {
                preparedStatement.setString(idIndex++, (String) field.get(t));
            } else if (field.getType() == int.class) {
                preparedStatement.setInt(idIndex++, (int) field.get(t));
            } else if (field.getType() == LocalDate.class) {
                Date date = Date.valueOf((LocalDate) field.get(t));
                preparedStatement.setDate(idIndex++, date);
            }
        }
        preparedStatement.executeUpdate();
    }
}
