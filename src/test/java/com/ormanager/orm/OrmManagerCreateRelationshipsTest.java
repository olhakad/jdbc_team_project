package com.ormanager.orm;

import com.ormanager.orm.annotation.*;
import com.ormanager.orm.test_entities.TestClassBook;
import com.ormanager.orm.test_entities.TestClassPublisher;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.ormanager.orm.OrmManagerUtil.getRelationshipFields;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j(topic = "CreateRelationshipsTest")
public class OrmManagerCreateRelationshipsTest {

    private static OrmManager manager;
    private static Class<TestClassBook> testClassBook = TestClassBook.class;
    private static Class<TestClassPublisher> testClassPublisher = TestClassPublisher.class;

    @BeforeAll
    static void setManager() throws SQLException {
        manager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
    }

    @AfterAll
    static void cleanMemory() {
        testClassBook = null;
        testClassPublisher = null;
    }

    @AfterEach
    void cleanDatabase() {
        LOGGER.info("Cleaning database...");
        manager.dropEntity(testClassBook);
        manager.dropEntity(testClassPublisher);
    }

    @Test
    @DisplayName("1. When 'getRelationshipFields' method is invoked for particular class then it should return all relation fields of this class")
    void test1() throws NoSuchFieldException {
        //Given
        var oneToManyFieldFromTestPublisherClass = testClassPublisher.getDeclaredField("books");
        var manyToOneFieldFromTestBookClass = testClassBook.getDeclaredField("publisher");

        //When
        var testPublisherClassRelationshipsFields = getRelationshipFields(testClassPublisher, OneToMany.class);
        var testBookClassRelationshipsFields = getRelationshipFields(testClassBook, ManyToOne.class);

        //Then
        assertEquals(1, testPublisherClassRelationshipsFields.size());
        assertEquals(1, testBookClassRelationshipsFields.size());
        assertEquals(oneToManyFieldFromTestPublisherClass, testPublisherClassRelationshipsFields.get(0));
        assertEquals(manyToOneFieldFromTestBookClass, testBookClassRelationshipsFields.get(0));
    }

    @Test
    @DisplayName("2. When 'doesRelationship' method is invoked for entities without created relation then it should return false")
    void test2() throws SQLException, NoSuchFieldException {
        //Given
        manager.register(testClassBook, testClassPublisher);

        //When
        var doesRelationshipExistResult = manager.doesRelationshipAlreadyExist(testClassBook, testClassPublisher);

        //Then
        assertFalse(doesRelationshipExistResult);
    }

    @Test
    @DisplayName("3. When 'doesRelationship' method is invoked for entities with created relation then it should return true")
    void test3() throws SQLException, NoSuchFieldException {
        //Given
        manager.register(testClassBook, testClassPublisher);
        manager.createRelationships(testClassBook, testClassPublisher);

        //When
        var doesRelationshipExistResult = manager.doesRelationshipAlreadyExist(testClassBook, testClassPublisher);

        //Then
        assertTrue(doesRelationshipExistResult);
    }

    @Test
    @DisplayName("4. When 'createRelationships' method is invoked for entities with mutual relation fields then it should create such a relationship")
    void test4() throws SQLException, NoSuchFieldException {
        //Given
        manager.register(testClassBook, testClassPublisher);

        //When
        manager.createRelationships(testClassBook, testClassPublisher);

        //Then
        var isRelationshipCreated = manager.doesRelationshipAlreadyExist(testClassBook, testClassPublisher);

        assertTrue(isRelationshipCreated);
    }
}
