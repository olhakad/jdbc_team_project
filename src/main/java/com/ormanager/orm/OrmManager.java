package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToObject;

public class OrmManager<T> {
    private Connection con;

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
                .concat(getAllValuesFromListToString(t))
                .concat(") VALUES(")
                .concat(questionMarks)
                .concat(");");

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
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

    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        T t = null;
        String sqlStatement = "SELECT * FROM "
                .concat(cls.getSimpleName())
                .concat(" WHERE id=?;");

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlStatement)) {
            t = cls.getDeclaredConstructor().newInstance();

            /*var idField = Arrays.stream(id.getClass().getDeclaredFields())
                    .filter(v -> v.isAnnotationPresent(Id.class))
                    .findFirst()
                    .get();*/

           /* idField.setAccessible(true);
            Long idFromField = (Long) idField.get("id");*/

            preparedStatement.setLong(1, (Long) id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                mapperToObject(resultSet, t);
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(t);
    }

    public static void main(String[] args) throws NoSuchMethodException, SQLException {

        Long id = 1L;
        Book book = new Book("kjshahfh", LocalDate.now());
        Publisher publisher =new Publisher("j7777777777h");
        /*Type returnType = OrmManager.class.getMethod("getBy", null)
                .getGenericReturnType().getClass();
        System.out.println(returnType.getTypeName());*/

        OrmManager<Book> ormManager = new OrmManager<>();
        OrmManager<Publisher> ormManager2 = new OrmManager<>();
       // System.out.println(ormManager.save(book));
        System.out.println(ormManager.findById(1L, Book.class));
        // System.out.println(id.getClass().getCanonicalName());

        //System.out.println(ormManager2.save(publisher));
    }
}
