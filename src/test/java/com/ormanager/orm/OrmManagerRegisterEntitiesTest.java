package com.ormanager.orm;

import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j(topic = "RegisterEntitiesTest")
public class OrmManagerRegisterEntitiesTest {

    private static OrmManager<?> manager;
    private static Class<TestClassBook> testClassBook = TestClassBook.class;
    private static final List<Method> privateMethodsOfOrmManager = new ArrayList<>();


    public OrmManagerRegisterEntitiesTest() {
    }

    @BeforeAll
    static void setAccessToPrivateMethodOfOrmManager() throws SQLException {
        manager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");

        for (var method : manager.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            privateMethodsOfOrmManager.add(method);
        }
    }

    @AfterAll
    static void cleanDatabase() throws SQLException, IllegalAccessException {
        deleteEntityFromDatabaseForTestPurpose(testClassBook);
        testClassBook = null;
    }

    @Test
    @DisplayName("1. Method: 'doesEntityExists' - result: false")
    void test1() throws InvocationTargetException, IllegalAccessException, SQLException {
        //Given
        deleteEntityFromDatabaseForTestPurpose(testClassBook);

        //When
        var doesEntityExistsMethod = getPrivateMethod("doesEntityExists");
        var testedMethodResult_boolean = doesEntityExistsMethod.invoke(manager, testClassBook);

        //Then
        assertFalse((Boolean) testedMethodResult_boolean);
    }

    @Test
    @DisplayName("2. Method: 'doesEntityExists' - result: true")
    void test2() throws SQLException, InvocationTargetException, IllegalAccessException {
        //Given
        manager.register(testClassBook);

        //When
        var doesEntityExistsMethod = getPrivateMethod("doesEntityExists");
        var testedMethodResult_boolean = doesEntityExistsMethod.invoke(manager, testClassBook);

        //Then
        assertTrue((Boolean) testedMethodResult_boolean);

        //CleanUpDataBase
        deleteEntityFromDatabaseForTestPurpose(testClassBook);
    }

    @Test
    @DisplayName("3. Method: 'getTableName(clazz)'")
    void test3() throws InvocationTargetException, IllegalAccessException {
        //Given
        var bookClassNameFromTableAnnotation = testClassBook.getAnnotation(Table.class).name();

        //When
        var getTableNameMethod = getPrivateMethod("getTableName");
        var testedMethodResult_string = getTableNameMethod.invoke(manager, testClassBook);

        //Then
        assertEquals(bookClassNameFromTableAnnotation, testedMethodResult_string);
    }

    @Test
    @DisplayName("4. Method: 'getIdFieldName(clazz)'")
    void test4() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        //Given
        var bookClassIdFieldName = testClassBook.getDeclaredField("id").getName();

        //When
        var getIdFieldNameMethod = getPrivateMethod("getIdFieldName");
        var testedMethodResult_string = getIdFieldNameMethod.invoke(manager, testClassBook);

        //Then
        assertEquals(bookClassIdFieldName, testedMethodResult_string);
    }

    @Test
    @DisplayName("5. Method: 'getBasicFieldsFromClass(clazz)'")
    void test5() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        //Given
        var basicFieldsFromTestClass = new ArrayList<>();
        basicFieldsFromTestClass.add(testClassBook.getDeclaredField("title"));
        basicFieldsFromTestClass.add(testClassBook.getDeclaredField("publishedAt"));

        //When
        var getBasicFieldsFromClassMethod = getPrivateMethod("getBasicFieldsFromClass");
        var testedMethodResult_list = getBasicFieldsFromClassMethod.invoke(manager, testClassBook);
        var result1 = testedMethodResult_list instanceof List ? ((List<?>) testedMethodResult_list).get(0) : null;
        var result2 = testedMethodResult_list instanceof List ? ((List<?>) testedMethodResult_list).get(1) : null;

        //Then
        assertEquals(basicFieldsFromTestClass.get(0), result1);
        assertEquals(basicFieldsFromTestClass.get(1), result2);
    }

    @Test
    @DisplayName("6. Method: 'getSqlTypeForField(field)'")
    void test6() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        //Given
        var longField = testClassBook.getDeclaredField("id");
        var stringField = testClassBook.getDeclaredField("title");
        var localDateField = testClassBook.getDeclaredField("publishedAt");

        //When
        var getSqlTypeForFieldMethod = getPrivateMethod("getSqlTypeForField");
        var testedMethodResultForLongField_string = getSqlTypeForFieldMethod.invoke(manager,longField);
        var testedMethodResultForStringField_string = getSqlTypeForFieldMethod.invoke(manager, stringField);
        var testedMethodResultForLocalDateField_string = getSqlTypeForFieldMethod.invoke(manager, localDateField);

        //Then
        assertEquals(" BIGINT,", testedMethodResultForLongField_string);
        assertEquals(" VARCHAR(255),", testedMethodResultForStringField_string);
        assertEquals(" DATE,", testedMethodResultForLocalDateField_string);
    }

    @Test
    @DisplayName("7. Method: 'register(clazz)")
    void test7() throws SQLException, InvocationTargetException, IllegalAccessException {
        //Given
        deleteEntityFromDatabaseForTestPurpose(testClassBook);

        //When
        manager.register(testClassBook);

        //Then
        var doesEntityExistsMethod = getPrivateMethod("doesEntityExists");
        var testedMethodResult_boolean = doesEntityExistsMethod.invoke(manager, testClassBook);

        assertTrue((Boolean) testedMethodResult_boolean);
    }

    private Method getPrivateMethod(String methodName) {
        return privateMethodsOfOrmManager.stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
    }

    private static void deleteEntityFromDatabaseForTestPurpose(Class<?> clazz) throws SQLException, IllegalAccessException {
        var clazzNameFromTableAnnotation = clazz.getAnnotation(Table.class).name();
        var clazzName = clazzNameFromTableAnnotation.equals("") ? clazz.getSimpleName().toLowerCase() : clazzNameFromTableAnnotation;

        var dropSQL = "DROP TABLE " + clazzName;

        var connectionField = Arrays.stream(manager.getClass().getDeclaredFields())
                .filter(field -> field.getType() == Connection.class)
                .findFirst()
                .orElseThrow();

        connectionField.setAccessible(true);

        var connection = (Connection) connectionField.get(manager);

        try (PreparedStatement dropStatement = connection.prepareStatement(dropSQL)) {
            dropStatement.execute();
            LOGGER.info(clazzName + " - test entity has been dropped from DB.");
        } catch (SQLException noTableFound) {
            LOGGER.info(clazzName + " - this entity already doesn't exist in DB.");
        }
    }

    @Entity
    @Table(name = "books")
    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    public static class TestClassBook {

        @Id
        private Long id;

        @NonNull
        private String title;

        @Column(name = "published_at")
        @NonNull
        private LocalDate publishedAt;

        @ManyToOne()
        Publisher publisher = null;
    }
}
