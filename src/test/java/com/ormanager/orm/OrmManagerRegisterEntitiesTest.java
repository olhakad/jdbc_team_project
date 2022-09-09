package com.ormanager.orm;

import com.ormanager.orm.test_entities.TestClassBook;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.ArrayList;

import static com.ormanager.orm.OrmManagerUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j(topic = "RegisterEntitiesTest")
public class OrmManagerRegisterEntitiesTest {

    private static OrmManager manager;
    private static Class<TestClassBook> testClassBook = TestClassBook.class;

    @BeforeAll
    static void setManager() throws SQLException {
        manager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
    }

    @AfterAll
    static void cleanMemory() {
        testClassBook = null;
    }

    @AfterEach
    void cleanDatabase() {
        LOGGER.info("Cleaning database...");
        manager.dropEntity(testClassBook);
    }

    @Test
    @DisplayName("1. When entity is not registered then 'doesEntityExist' method should return false")
    void test1() throws SQLException {
        //Given
        manager.dropEntity(testClassBook);

        //When
        var doesEntityExistMethodResult = manager.doesEntityExist(testClassBook);

        //Then
        assertFalse(doesEntityExistMethodResult);
    }

    @Test
    @DisplayName("2. When entity is registered then 'doesEntityExist' method should return true")
    void test2() throws SQLException, NoSuchFieldException {
        //Given
        manager.register(testClassBook);

        //When
        var doesEntityExistMethodResult = manager.doesEntityExist(testClassBook);

        //Then
        assertTrue(doesEntityExistMethodResult);
    }

    @Test
    @DisplayName("3. When 'getTableName' method is invoked for particular class then it should return String name from table annotation of this class")
    void test3() {
        //Given
        var testClassBookTableName = "test_books";

        //When
<<<<<<< HEAD
        var getTableNameMethodResult = OrmManagerUtil.getTableName(testClassBook);
=======
        var getTableNameMethodResult = getTableName(testClassBook);
>>>>>>> 1916ad2cd8cc84c6a639426d4bf75e550c41e59b

        //Then
        assertEquals(testClassBookTableName, getTableNameMethodResult);
    }

    @Test
    @DisplayName("4. When 'getIdFieldName' method is invoked for particular class then it should return String id field name of this class")
    void test4() throws NoSuchFieldException {
        //Given
        var testClassBookIdFieldName = "id";

        //When
<<<<<<< HEAD
        var getIdFieldNameMethodResult = OrmManagerUtil.getIdFieldName(testClassBook);
=======
        var getIdFieldNameMethodResult = getIdFieldName(testClassBook);
>>>>>>> 1916ad2cd8cc84c6a639426d4bf75e550c41e59b

        //Then
        assertEquals(testClassBookIdFieldName, getIdFieldNameMethodResult);
    }

    @Test
    @DisplayName("5. When 'getBasicFieldsFromClass' method is invoked for particular class then it should return List of class's fields except: id-like, relation-like, collection-like fields for this class")
    void test5() throws NoSuchFieldException {
        //Given
        var basicFieldsFromTestClass = new ArrayList<>();
        basicFieldsFromTestClass.add(testClassBook.getDeclaredField("title"));
        basicFieldsFromTestClass.add(testClassBook.getDeclaredField("publishedAt"));

        //When
<<<<<<< HEAD
        var getBasicFieldsFromClassMethodResult = OrmManagerUtil.getBasicFieldsFromClass(testClassBook);
=======
        var getBasicFieldsFromClassMethodResult = getBasicFieldsFromClass(testClassBook);
>>>>>>> 1916ad2cd8cc84c6a639426d4bf75e550c41e59b

        //Then
        assertEquals(basicFieldsFromTestClass.get(0), getBasicFieldsFromClassMethodResult.get(0));
        assertEquals(basicFieldsFromTestClass.get(1), getBasicFieldsFromClassMethodResult.get(1));
    }

    @Test
    @DisplayName("6. When 'getSqlTypeForField' method is invoked then it should return String of correct SQL type for particular java fields")
    void test6() throws NoSuchFieldException {
        //Given
        var longField = testClassBook.getDeclaredField("id");
        var stringField = testClassBook.getDeclaredField("title");
        var localDateField = testClassBook.getDeclaredField("publishedAt");

        //When
<<<<<<< HEAD
        var getSqlTypeForFieldMethodResultForLong = OrmManagerUtil.getSqlTypeForField(longField);
        var getSqlTypeForFieldMethodResultForString = OrmManagerUtil.getSqlTypeForField(stringField);
        var getSqlTypeForFieldMethodResultForLocalDate = OrmManagerUtil.getSqlTypeForField(localDateField);
=======
        var getSqlTypeForFieldMethodResultForLong = getSqlTypeForField(longField);
        var getSqlTypeForFieldMethodResultForString = getSqlTypeForField(stringField);
        var getSqlTypeForFieldMethodResultForLocalDate = getSqlTypeForField(localDateField);
>>>>>>> 1916ad2cd8cc84c6a639426d4bf75e550c41e59b


        //Then
        assertEquals(" BIGINT,", getSqlTypeForFieldMethodResultForLong);
        assertEquals(" VARCHAR(255),", getSqlTypeForFieldMethodResultForString);
        assertEquals(" DATE,", getSqlTypeForFieldMethodResultForLocalDate);
    }

    @Test
    @DisplayName("7. When 'register' method is invoked then it should register entity in database")
    void test7() throws SQLException, NoSuchFieldException {
        //Given
        manager.dropEntity(testClassBook);

        //When
        manager.register(testClassBook);

        //Then
        var doesEntityExistMethodResult = manager.doesEntityExist(testClassBook);

        assertTrue(doesEntityExistMethodResult);
    }
}
