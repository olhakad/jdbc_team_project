package com.ormanager.orm;

import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j(topic = "OrmManager")
public class OrmManager<T> {
    private Connection con;
    private AtomicLong id = new AtomicLong(0L);
    private int idIndex = 1;

    public static <T> OrmManager<T> getConnection() throws SQLException {
        return new OrmManager<T>();
    }

    private OrmManager() throws SQLException {
        this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "");
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

    public boolean delete(T recordToDelete) {
        boolean isDeleted = false;
        if (isRecordInDataBase(recordToDelete)) {
            String tableName = recordToDelete.getClass().getAnnotation(Table.class).name();
            String queryCheck = String.format("DELETE FROM %s WHERE id = ?", tableName);

            try (PreparedStatement preparedStatement = con.prepareStatement(queryCheck)) {
                String recordId = getRecordId(recordToDelete);
                preparedStatement.setString(1, recordId);
                LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

                isDeleted = preparedStatement.executeUpdate() > 0;
            } catch (SQLException | IllegalAccessException e) {
                LOGGER.error(String.valueOf(e));
            }

            if (isDeleted) {
                setIdToZero(recordToDelete);
            }
        }
        return isDeleted;
    }

    private void setIdToZero(T targetObject) {
        Arrays.stream(targetObject.getClass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            try {
                field.set(targetObject, null);
            } catch (IllegalAccessException e) {
                LOGGER.error(String.valueOf(e));
            }
        });
    }

    private boolean isRecordInDataBase(T searchedRecord) {
        boolean isInDB = false;
        String tableName = searchedRecord.getClass().getAnnotation(Table.class).name();
        String queryCheck = String.format("SELECT count(*) FROM %s WHERE id = ?", tableName);

        try (PreparedStatement preparedStatement = con.prepareStatement(queryCheck)) {
            String recordId = getRecordId(searchedRecord);

            preparedStatement.setString(1, recordId);
            LOGGER.info("SQL CHECK STATEMENT: {}", preparedStatement);

            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                isInDB = count == 1;
            }
        } catch (SQLException | IllegalAccessException e) {
            LOGGER.error(String.valueOf(e));
        }

        LOGGER.info("This {} {} in Data Base.",
                searchedRecord.getClass().getSimpleName(),
                isInDB ? "exists" : "does not exist");

        return isInDB;
    }

    private String getRecordId(T recordInDb) throws IllegalAccessException {
        Optional<Field> optionalId = Arrays.stream(recordInDb.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findAny();
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            return optionalId.get().get(recordInDb).toString();
        }
        return "";
    }
}
