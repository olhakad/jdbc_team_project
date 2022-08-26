package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;

import java.lang.reflect.Field;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    /*   public void persist(T t) throws IllegalArgumentException, SQLException, IllegalAccessException {
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
                   .collect(Collectors.joining(","));//" + qMarks + "
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
       }*/
    public void persist(T t) {
        var length = getAllDeclaredFieldsFromObject(t).size()-1;
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
            for (Field field : getAllValuesFromObjectweyweagarga(t)) {
                field.setAccessible(true);

                var index = getAllValuesFromObjectweyweagarga(t).indexOf(field)+1;

                /*if (field.getType() == Long.class) {
                    preparedStatement.setLong(index, (Long) field.get(t));
                } else */if (field.getType() == String.class) {
                    preparedStatement.setString(index, (String) field.get(t));
                } else if (field.getType() == LocalDate.class) {
                    Date date = Date.valueOf((LocalDate) field.get(t));
                    preparedStatement.setDate(index, date);
                }
            }
            preparedStatement.executeUpdate();
            connection.commit();
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

    public String getIdName(T t) {
        return getAllDeclaredFieldsFromObject(t).stream()
                .filter(v -> v.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .findAny()
                .orElse("id");
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

    public List<Field> getAllValuesFromObjectweyweagarga(T t) {
        List<Field> strings = new ArrayList<>();
        for (Field field : getAllDeclaredFieldsFromObject(t)) {
            if (!field.isAnnotationPresent(Id.class)) {
                /*if (field.isAnnotationPresent(Column.class)
                        && !Objects.equals(field.getDeclaredAnnotation(Column.class).name(), "")) {
                    strings.add( field.getDeclaredAnnotation(Column.class));
                } else {*/
                    strings.add(field);
                }
            }
       // }
        return strings;
    }

    public static void main(String[] args) throws SQLException, IllegalAccessException {

        Book book = new Book();
        Publisher publisher = new Publisher();
        //Class<?> clazz = book.getClass();

        //Arrays.stream(book.getClass().getDeclaredFields()).forEach(System.out::println);
        //Field[] fields = clazz.getClass().getDeclaredFields();
        OrmManager<Book> orm = new OrmManager<>();
        OrmManager<Publisher> orm2 = new OrmManager<>();
        //System.out.println(orm.getAllValuesFromObject(book));
        //System.out.println(orm2.getAllValuesFromObject(publisher));
        // System.out.println(orm2.save(publisher));

        //System.out.println(Arrays.toString(fields));
        Publisher publisher1 = new Publisher();
        Publisher publisher2 = new Publisher();
        Publisher publisher3 = new Publisher();
        Book book1 = new Book("ijh", LocalDate.now());
        Book book2 = new Book( "ijffh", LocalDate.now());
        Book book3 = new Book("ifgddjh", LocalDate.now());
        OrmManager<Book> ormManager = new OrmManager<>();
        ormManager.persist(book1);
        ormManager.persist(book2);
        ormManager.persist(book3);
       /* Publisher publisher = new Publisher();
        Publisher publisher2 = new Publisher();
        Publisher publisher3 = new Publisher();
        OrmManager<Publisher> orm = new OrmManager<>();
        orm.persist(publisher);
        orm.persist(publisher2);
        orm.persist(publisher3);*/
    }
}
