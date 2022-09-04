package com.ormanager.orm;

import com.ormanager.orm.annotation.Table;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j(topic = "RegisterEntitiesTest")
public class OrmManagerRegisterEntitiesTest {

    private static OrmManager<?> manager;
    private static final Class<?> managerClass = OrmManager.class;
    private static Class<TestRequiredLogic.TestClassBook> testClassBook = TestRequiredLogic.TestClassBook.class;


    public OrmManagerRegisterEntitiesTest() {
    }

    @BeforeAll
    static void setManager() throws SQLException {
        manager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
    }

    @AfterAll
    static void cleanDatabase() throws IllegalAccessException {
        LOGGER.info("Cleaning database...");
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassBook, manager);
        testClassBook = null;
    }

    @Test
    @DisplayName("1. Method: 'doesEntityExists' - result: false")
    void test1() throws InvocationTargetException, IllegalAccessException {
        //Given
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassBook, manager);

        //When
        var doesEntityExistsMethod = TestRequiredLogic.getPrivateMethod("doesEntityExists", managerClass);
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
        var doesEntityExistsMethod = TestRequiredLogic.getPrivateMethod("doesEntityExists", managerClass);
        var testedMethodResult_boolean = doesEntityExistsMethod.invoke(manager, testClassBook);

        //Then
        assertTrue((Boolean) testedMethodResult_boolean);

        //CleanUpDataBase
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassBook, manager);
    }

    @Test
    @DisplayName("3. Method: 'getTableName(clazz)'")
    void test3() throws InvocationTargetException, IllegalAccessException {
        //Given
        var bookClassNameFromTableAnnotation = testClassBook.getAnnotation(Table.class).name();

        //When
        var getTableNameMethod = TestRequiredLogic.getPrivateMethod("getTableName", managerClass);
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
        var getIdFieldNameMethod = TestRequiredLogic.getPrivateMethod("getIdFieldName", managerClass);
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
        var getBasicFieldsFromClassMethod = TestRequiredLogic.getPrivateMethod("getBasicFieldsFromClass", managerClass);
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
        var getSqlTypeForFieldMethod = TestRequiredLogic.getPrivateMethod("getSqlTypeForField", managerClass);
        var testedMethodResultForLongField_string = getSqlTypeForFieldMethod.invoke(manager,longField);
        var testedMethodResultForStringField_string = getSqlTypeForFieldMethod.invoke(manager, stringField);
        var testedMethodResultForLocalDateField_string = getSqlTypeForFieldMethod.invoke(manager, localDateField);

        //Then
        assertEquals(" BIGINT,", testedMethodResultForLongField_string);
        assertEquals(" VARCHAR(255),", testedMethodResultForStringField_string);
        assertEquals(" DATE,", testedMethodResultForLocalDateField_string);
    }

    @Test
    @DisplayName("7. Method: 'register(clazz)'")
    void test7() throws SQLException, InvocationTargetException, IllegalAccessException {
        //Given
        TestRequiredLogic.deleteEntityFromDatabaseForTestPurpose(testClassBook, manager);

        //When
        manager.register(testClassBook);

        //Then
        var doesEntityExistsMethod = TestRequiredLogic.getPrivateMethod("doesEntityExists", managerClass);
        var testedMethodResult_boolean = doesEntityExistsMethod.invoke(manager, testClassBook);

        assertTrue((Boolean) testedMethodResult_boolean);
    }
}
