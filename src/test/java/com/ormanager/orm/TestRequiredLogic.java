package com.ormanager.orm;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "TestRequiredLogic")
public class TestRequiredLogic {

    static Method getPrivateMethod(String methodName, Class<?> targetClass) {
        var method = Arrays.stream(targetClass.getDeclaredMethods())
                .filter(method1 -> method1.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        method.setAccessible(true);

        return method;
    }

    static void deleteEntityFromDatabaseForTestPurpose(Class<?> clazz, OrmManager<?> manager) throws IllegalAccessException {
        var entityNameFromTableAnnotation = clazz.getAnnotation(Table.class).name();
        var entityName = entityNameFromTableAnnotation.equals("") ? clazz.getSimpleName().toLowerCase() : entityNameFromTableAnnotation;

        var dropSQL = "DROP TABLE " + entityName;

        var connectionField = Arrays.stream(manager.getClass().getDeclaredFields())
                .filter(field -> field.getType() == Connection.class)
                .findFirst()
                .orElseThrow();

        connectionField.setAccessible(true);

        var connection = (Connection) connectionField.get(manager);

        try (PreparedStatement dropStatement = connection.prepareStatement(dropSQL)) {
            dropStatement.execute();
            LOGGER.info(entityName + " - test entity has been dropped from DB.");
        } catch (SQLException noTableFound) {
            LOGGER.info(entityName + " - this entity already doesn't exist in DB.");
        }
    }

    @Entity
    @Table(name = "test_books")
    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    static class TestClassBook {

        @Id
        private Long id;

        @NonNull
        private String title;

        @Column(name = "published_at")
        @NonNull
        private LocalDate publishedAt;

        @ManyToOne(columnName = "publisher_id")
        TestClassPublisher publisher = null;
    }


    @Entity
    @Table(name = "test_publishers")
    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    static class TestClassPublisher implements Serializable {
        @Id
        private Long id;

        @NonNull
        private String name;

        @OneToMany(mappedBy = "publisher")
        private List<TestClassBook> books = new ArrayList<>();
    }
}
