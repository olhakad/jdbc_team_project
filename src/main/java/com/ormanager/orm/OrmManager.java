package com.ormanager.orm;


import com.ormanager.orm.annotation.*;
import com.ormanager.jdbc.DataSource;
import com.ormanager.orm.mapper.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;

public class OrmManager<T> {
    private Connection con;
    Logger logger = LoggerFactory.getLogger(OrmManager.class);

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DataSource.getConnection();
    }

    public void persist(T t) throws SQLException, IllegalAccessException {
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
            for (Field field : getAllColumnsButId(t)) {
                field.setAccessible(true);

                var index = getAllColumnsButId(t).indexOf(field) + 1;

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
        }
    }

    public T save(T t) throws SQLException, IllegalAccessException {
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

    public List<Field> getAllColumnsButId(T t) {
        return Arrays.stream(t.getClass().getDeclaredFields())
                .filter(v -> !v.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        T t = null;
        String sqlStatement = "SELECT * FROM "
                .concat(cls.getDeclaredAnnotation(Table.class).name())
                .concat(" WHERE id=")
                .concat(id.toString())
                .concat(";");
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            t = cls.getDeclaredConstructor().newInstance();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                t = mapperToObject(resultSet, t).orElseThrow();
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(t);
    }

    public List<T> findAll(Class<T> cls) throws SQLException {
        List<T> allEntities = new ArrayList<>();
        String sqlStatement = "SELECT * FROM " + cls.getAnnotation(Table.class).name();
        logger.info("sqlStatement {}", sqlStatement);
        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                T t = cls.getConstructor().newInstance();
                ObjectMapper.mapperToObject(resultSet, t);
                allEntities.add(t);
            }

            return allEntities;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    public Stream<T> findAllAsStream(Class<T> cls) {
        try {
            return findAll(cls).stream();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
