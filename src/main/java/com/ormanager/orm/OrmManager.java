package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.jdbc.DataSource;
import com.ormanager.orm.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrmManager<T> {
    private Connection con;
    Logger logger = LoggerFactory.getLogger(OrmManager.class);

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DataSource.getConnection();
    }

    public void persist(T t) {
        var length = getAllDeclaredFieldsFromObject(t).size() - 1;
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
        logger.info("SQL STATEMENT : {}", sqlStatement);
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            for (Field field : getAllColumns(t)) {
                field.setAccessible(true);

                var index = getAllColumns(t).indexOf(field) + 1;

                if (field.getType() == String.class) {
                    preparedStatement.setString(index, (String) field.get(t));
                } else if (field.getType() == LocalDate.class) {
                    Date date = Date.valueOf((LocalDate) field.get(t));
                    preparedStatement.setDate(index, date);
                } else if (field.getName().equals("books") || field.getName().equals("publisher")) {
                    preparedStatement.setString(index, (String) "");
                }
            }
            logger.info("PREPARED STATEMENT : {}", preparedStatement);
            preparedStatement.executeUpdate();
        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public T save(T t) {
        persist(t);
        return t;
    }

    public List<T> findAll(T t) throws SQLException {
        List<T> allEntities = new ArrayList<>();
        String sqlStatement = "SELECT * FROM " + getTableClassName(t);
        logger.info("sqlStatement {}", sqlStatement);
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                LocalDate publishedAt = resultSet.getDate("published_at").toLocalDate();
                String publisher = resultSet.getString("publisher");
                logger.info("id: {} title: {} publishedAt: {}", id, title, publishedAt);
                allEntities.add()
            }
        }
        logger.info("resultSet: {}");
        return allEntities;
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