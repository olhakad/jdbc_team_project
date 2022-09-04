package com.ormanager.orm;

import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.OneToMany;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j(topic = "CreateRelationshipsTest")
public class OrmManagerCreateRelationshipsTest {

    private static OrmManager<?> manager;
    private static final Class<?> managerClass = OrmManager.class;
    private static Class<TestRequiredLogic.TestClassBook> testClassBook = TestRequiredLogic.TestClassBook.class;
    private static Class<TestRequiredLogic.TestClassPublisher> testClassPublisher = TestRequiredLogic.TestClassPublisher.class;

    @BeforeAll
    static void setManager() throws SQLException {
        manager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
    }

    @AfterAll
    static void cleanMemory() {
        testClassBook = null;
        testClassPublisher = null;
    }

    @AfterEach
    void cleanDatabase() throws IllegalAccessException {
        LOGGER.info("Cleaning database...");
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassBook, manager);
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassPublisher, manager);
    }

    @Test
    @DisplayName("1. Method: 'getRelationshipFields(clazz, relationAnnotation)'")
    void test1() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        //Given
        var oneToManyFieldFromTestPublisherClass = testClassPublisher.getDeclaredField("books");
        var manyToOneFieldFromTestBookClass = testClassBook.getDeclaredField("publisher");

        //When
        var getRelationshipFieldsMethod = TestRequiredLogic.getPrivateMethod("getRelationshipFields", managerClass);
        var testedMethodResultForTestPublisherClass_list = (List<?>) getRelationshipFieldsMethod.invoke(manager, testClassPublisher, OneToMany.class);
        var testedMethodResultForTestBookClass_list = (List<?>) getRelationshipFieldsMethod.invoke(manager, testClassBook, ManyToOne.class);

        //Then
        assertEquals(1, testedMethodResultForTestPublisherClass_list.size());
        assertEquals(1, testedMethodResultForTestBookClass_list.size());
        assertEquals(oneToManyFieldFromTestPublisherClass, testedMethodResultForTestPublisherClass_list.get(0));
        assertEquals(manyToOneFieldFromTestBookClass, testedMethodResultForTestBookClass_list.get(0));
    }

    @Test
    @DisplayName("2. Method: 'doesRelationshipAlreadyExist(clazzToCheck, relationToCheck)' - false")
    void test2() throws SQLException, InvocationTargetException, IllegalAccessException {
        //Given
        manager.register(testClassBook, testClassPublisher);

        //When
        var doesRelationshipAlreadyExistMethod = TestRequiredLogic.getPrivateMethod("doesRelationshipAlreadyExist", managerClass);
        var testedMethodResult_boolean = doesRelationshipAlreadyExistMethod.invoke(manager, testClassBook, testClassPublisher);

        //Then
        assertFalse((Boolean) testedMethodResult_boolean);
    }

    @Test
    @DisplayName("3. Method: 'createRelationships(clazz)'")
    void test3() throws SQLException, IllegalAccessException {
        //Given
        manager.register(testClassBook, testClassPublisher);
        var isRelationshipCreated = false;

        //When
        manager.createRelationships(testClassBook, testClassPublisher);

        //Then
        String checkRelationshipsSQL = "SELECT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = 'test_books'";

        var connectionField = Arrays.stream(manager.getClass().getDeclaredFields())
                .filter(field -> field.getType() == Connection.class)
                .findFirst()
                .orElseThrow();

        connectionField.setAccessible(true);

        var connection = (Connection) connectionField.get(manager);

        try (PreparedStatement statement = connection.prepareStatement(checkRelationshipsSQL)) {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();

            while (resultSet.next()) {
                if (resultSet.getString(1).equals("test_publishers")) {
                    isRelationshipCreated = true;
                    break;
                }
            }
        }

        assertTrue(isRelationshipCreated);
    }

    @Test
    @DisplayName("4. Method: 'doesRelationshipAlreadyExist(clazzToCheck, relationToCheck)' - true")
    void test4() throws SQLException, InvocationTargetException, IllegalAccessException {
        //Given
        manager.register(testClassBook, testClassPublisher);
        manager.createRelationships(testClassBook, testClassPublisher);

        //When
        var doesRelationshipAlreadyExistMethod = TestRequiredLogic.getPrivateMethod("doesRelationshipAlreadyExist", managerClass);
        var testedMethodResult_boolean = doesRelationshipAlreadyExistMethod.invoke(manager, testClassBook, testClassPublisher);

        //Then
        assertTrue((Boolean) testedMethodResult_boolean);
    }
}
