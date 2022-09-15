package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.jdbc.ConnectionToDB;
import com.ormanager.orm.test_entities.TestClassBookWithUuidId;
import com.ormanager.orm.test_entities.TestClassPublisherWithUuidId;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToList;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class UuidIdTest {

    private OrmManager ormManager;

    @BeforeEach
    void setUp() throws SQLException, NoSuchFieldException {
        ormManager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
        ormManager.dropEntity(TestClassBookWithUuidId.class);
        ormManager.dropEntity(TestClassPublisherWithUuidId.class);

        Class<?>[] classesToRegister = {TestClassPublisherWithUuidId.class, TestClassBookWithUuidId.class};

        ormManager.register(classesToRegister);
        ormManager.createRelationships(classesToRegister);
    }

    @Test
    @DisplayName("1. FindAllAsIterable should be lazy loading with UUID")
    void whenUsingFindAllAsIterableTest_ShouldBeLazyLoading_UUID_ID() throws Exception {
        //GIVEN
        TestClassPublisherWithUuidId publisher1 = new TestClassPublisherWithUuidId("saveTestPublisher1");
        TestClassPublisherWithUuidId publisher2 = new TestClassPublisherWithUuidId("saveTestPublisher2");
        TestClassPublisherWithUuidId publisher3 = new TestClassPublisherWithUuidId("saveTestPublisher3");

        //WHEN
        ormManager.getOrmCache().clearCache();
        ormManager.save(publisher1);
        ormManager.save(publisher2);
        ormManager.save(publisher3);
        ormManager.getOrmCache().deleteFromCache(publisher1);
        ormManager.getOrmCache().deleteFromCache(publisher2);
        ormManager.getOrmCache().deleteFromCache(publisher3);
        var iterator = ormManager.findAllAsIterable(TestClassPublisherWithUuidId.class);
        int counter = 0;
        while (iterator.hasNext() && counter < 1) {
            counter++;
            iterator.next();
        }
        iterator.close();

        //THEN
        assertEquals(ormManager.getOrmCache().count(TestClassPublisherWithUuidId.class), counter);
    }

    @Test
    @DisplayName("2. Should return autogenerated UUID from DB ")
    void save_ShouldReturnAutoGeneratedIdOfPublisherFromDatabase_UUID_ID() throws SQLException {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("test");
        UUID expectedId;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test.publishersWithUuidId WHERE id = (SELECT MAX(id) from publishersWithUuidId);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = UUID.fromString(resultSet.getString(1));
        }

        //THEN
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    @DisplayName("3. Should return autogenerated UUID from DB ")
    void save_ShouldReturnAutoGeneratedIdOfBookFromDatabase_UUID_ID() throws SQLException {
        //GIVEN
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("test", LocalDate.now());

        var pub = new TestClassPublisherWithUuidId("test");
        ormManager.save(pub);
        book.setPublisherWithUuidId(pub);
        UUID expectedId;

        //WHEN
        ormManager.save(book);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test.booksWithUuidId WHERE id = (SELECT MAX(id) from booksWithUuidId);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = UUID.fromString(resultSet.getString(1));
        }

        //THEN
        assertEquals(expectedId, book.getId());
    }

    @Test
    @DisplayName("4. Should return object by given UUID with findById from DB ")
    void findById_ShouldReturnPublisherFromDatabaseByGivenId_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("test");
        UUID expectedId = null;

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test.publishersWithUuidId WHERE id =\'" + id + "\';")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = UUID.fromString(resultSet.getString(1));
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(publisher, ormManager.findById(publisher.getId(), TestClassPublisherWithUuidId.class).orElseThrow());
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    @DisplayName("5. Should return object by given UUID with findById from DB ")
    void findById_ShouldReturnBookFromDatabaseByGivenId_UUID_ID() {
        //GIVEN
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("test", LocalDate.now());
        UUID expectedId = null;

        //WHEN
        var pub = new TestClassPublisherWithUuidId("test");
        ormManager.save(pub);
        book.setPublisherWithUuidId(pub);
        ormManager.save(book);

        var id = book.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test.booksWithUuidId WHERE id =\'" + id + "\';")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = UUID.fromString(resultSet.getString(1));
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(book, ormManager.findById(book.getId(), TestClassBookWithUuidId.class).orElseThrow());
        assertEquals(expectedId, book.getId());
    }

    @Test
    @DisplayName("6. Should thrown NoSuchElementException when given UUID is not exist")
    void findByIdPublisherNullValue_ShouldReturnNoSuchElementException_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(publisher.getId(), TestClassPublisherWithUuidId.class));
    }

    @Test
    @DisplayName("7. Should thrown NoSuchElementException when given UUID is not exist")
    void findByIdBookNullValue_ShouldReturnNoSuchElementException_UUID_ID() {
        //GIVEN
        TestClassBookWithUuidId book = new TestClassBookWithUuidId();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(book.getId(), TestClassBookWithUuidId.class));
    }

    @Test
    @DisplayName("8. FindAll should return all Publishers")
    void findAllPublishersTest_UUID_ID() throws SQLException {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("saveTestPublisher");
        List<TestClassPublisherWithUuidId> publishers;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM test.publishersWithUuidId;");
            publishers = mapperToList(resultSet, TestClassPublisherWithUuidId.class);
        }
        var findAllList = ormManager.findAll(TestClassPublisherWithUuidId.class);

        //THEN
        assertTrue(publishers.size() > 0);
        assertEquals(findAllList.size(), publishers.size());
    }

    @Test
    @DisplayName("9. FindAll should return all Books")
    void findAllBooksTest_UUID_ID() throws SQLException {
        //GIVEN
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("test", LocalDate.now());
        List<Book> books;

        //WHEN
        var pub = new TestClassPublisherWithUuidId("test");
        ormManager.save(pub);
        book.setPublisherWithUuidId(pub);
        ormManager.save(book);

        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM test.booksWithUuidId;");
            books = mapperToList(resultSet, TestClassBookWithUuidId.class);
        }
        var findAllList = ormManager.findAll(TestClassBookWithUuidId.class);

        //THEN
        assertTrue(books.size() > 0);
        assertEquals(findAllList.size(), books.size());
    }

    @Test
    @DisplayName("10. FindAll should return all Books")
    void givenPublisherIsUpdated_thenAssertId_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("Test1");

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();
        TestClassPublisherWithUuidId publisher1 = ormManager.findById(id, TestClassPublisherWithUuidId.class).get();

        //THEN
        assertEquals(id, ((TestClassPublisherWithUuidId) ormManager.update(publisher1)).getId());
    }

    @Test
    @DisplayName("11. Object should be updated with UUID")
    void givenBookIsUpdated_thenAssertId_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("Test1");
        ormManager.save(publisher);
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("Harry Potter", LocalDate.now());
        book.setPublisherWithUuidId(publisher);

        //WHEN
        ormManager.save(book);
        var id = book.getId();
        TestClassBookWithUuidId book1 = ormManager.findById(id, TestClassBookWithUuidId.class).get();

        //THEN
        assertEquals(id, ((TestClassBookWithUuidId) ormManager.update(book1)).getId());
    }

    @Test
    @DisplayName("12. Object should be updated with UUID")
    void givenPublisherSetNewName_whenUpdatePublisher_thenAssertName_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("Test1");

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();
        TestClassPublisherWithUuidId publisher1 = ormManager.findById(id, TestClassPublisherWithUuidId.class).get();

        publisher1.setName("Test2");
        var name = ((TestClassPublisherWithUuidId) ormManager.update(publisher1)).getName();

        //THEN
        assertEquals("Test1", name);
    }

    @Test
    @DisplayName("13. Object should be updated with UUID")
    void givenBookSetNewTitle_whenUpdatePublisher_thenAssertTitle_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("Test2");
        ormManager.save(publisher);
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("Lord of the rings", LocalDate.now());
        book.setPublisherWithUuidId(publisher);

        //WHEN
        ormManager.save(book);
        var id = book.getId();
        TestClassBookWithUuidId book1 = ormManager.findById(id, TestClassBookWithUuidId.class).get();

        book1.setTitle("Alice in the wonderland");
        var title = ((TestClassBookWithUuidId) ormManager.update(book1)).getTitle();

        //THEN
        assertEquals("Lord of the rings", title);
    }

    @Test
    @DisplayName("14. When Publisher was deleted ID of Book should be set to null")
    void whenDeletingPublisher_ShouldDeletePublisherAndBooksAndSetIdToNull_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher= new TestClassPublisherWithUuidId("testPub");
        TestClassBookWithUuidId book= new TestClassBookWithUuidId("testBook", LocalDate.now());
        publisher.getBooksWithUuidId().add(book);
        ormManager.save(publisher);
        //WHEN
        ormManager.delete(publisher);
        //THEN
        assertNull(publisher.getId());
        assertNull(book.getId());
        assertFalse(ormManager.getOrmCache().isRecordInCache(publisher.getId(), TestClassPublisherWithUuidId.class));
        assertFalse(ormManager.getOrmCache().isRecordInCache(book.getId(), TestClassBookWithUuidId.class));
    }

    @Test
    @DisplayName("15. When Book was deleted ID of Publisher should be set to null")
    void whenDeletingBook_ShouldDeleteBookAndSetIdToNull_UUID_ID() {
        //GIVEN
        TestClassBookWithUuidId book =(TestClassBookWithUuidId) ormManager.save(new TestClassBookWithUuidId("testBook", LocalDate.now()));
        //WHEN
        ormManager.delete(book);
        //THEN
        assertNull(book.getId());
    }

    @Test
    @DisplayName("16. Publisher should be merged")
    void givenPublisherIsMerged_thenAssertResultAndName_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("testPub");
        ormManager.save(publisher);

        //WHEN
        var name = "Test123";
        publisher.setName(name);
        var expectedResult = ormManager.merge(publisher);
        var mergedName = ormManager.findById(publisher.getId(), TestClassPublisherWithUuidId.class).get().getName();

        //THEN
        assertTrue(expectedResult);
        assertEquals(name, mergedName);
    }

    @Test
    @DisplayName("17. Book should be merged")
    void givenBookIsMerged_thenAssertResultAndTitle_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("Test");
        ormManager.save(publisher);
        TestClassBookWithUuidId book = new TestClassBookWithUuidId("Lord of the rings", LocalDate.now());
        book.setPublisherWithUuidId(publisher);
        ormManager.save(book);

        //WHEN
        var title = "Alice in the wonderland";
        book.setTitle(title);
        var expectedResult = ormManager.merge(book);
        var mergedTitle = ormManager.findById(book.getId(), TestClassBookWithUuidId.class).get().getTitle();

        //THEN
        assertTrue(expectedResult);
        assertEquals(title, mergedTitle);
    }

    @Test
    @DisplayName("18. Publisher should be merged & Book should be saved")
    void givenPublisherGetBook_whenPublisherIsMerged_thenBookShouldBeSaved_UUID_ID() {
        //GIVEN
        ormManager.getOrmCache().clearCache();
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("testPub21");
        ormManager.save(publisher);
        TestClassBookWithUuidId book1 = new TestClassBookWithUuidId("Book11", LocalDate.now());
        publisher.getBooksWithUuidId().add(book1);
        ormManager.getOrmCache().clearCache();

        //WHEN
        var expectedResult = ormManager.merge(publisher);
        List<TestClassBookWithUuidId> lists = ormManager.findById(publisher.getId(), TestClassPublisherWithUuidId.class).get().getBooksWithUuidId();


        //THEN
        assertTrue(expectedResult);
        assertEquals(1, lists.size());
        assertEquals(lists.get(0).getPublisherWithUuidId(), publisher);
    }

    @Test
    @DisplayName("19. Should delete Publisher when it assign to Book")
    void whenDeletingPublisherWithAssignedBooks_ShouldDeleteAssignedBooksAndPublisher_UUID_ID() {
        //GIVEN
        TestClassPublisherWithUuidId publisher = new TestClassPublisherWithUuidId("test Publisher");
        TestClassBookWithUuidId book1 = new TestClassBookWithUuidId("book example 1", LocalDate.of(1979, 2, 23));
        TestClassBookWithUuidId book2 = new TestClassBookWithUuidId("book example 2", LocalDate.of(1989, 3, 22));
        TestClassBookWithUuidId book3 = new TestClassBookWithUuidId("book example 3", LocalDate.of(1999, 4, 21));
        publisher.setBooksWithUuidId(List.of(book1, book2, book3));
        TestClassPublisherWithUuidId savedPublisher = (TestClassPublisherWithUuidId) ormManager.save(publisher);
        var book1Id = ormManager.findById(book1.getId(), TestClassBookWithUuidId.class).get().getId();
        var book2Id = ormManager.findById(book2.getId(), TestClassBookWithUuidId.class).get().getId();
        var book3Id = ormManager.findById(book3.getId(), TestClassBookWithUuidId.class).get().getId();
        //WHEN
        ormManager.delete(savedPublisher);
        TestClassBookWithUuidId deletedBook1 = ormManager.findById(book1Id, TestClassBookWithUuidId.class).get();
        TestClassBookWithUuidId deletedBook2 = ormManager.findById(book2Id, TestClassBookWithUuidId.class).get();
        TestClassBookWithUuidId deletedBook3 = ormManager.findById(book3Id, TestClassBookWithUuidId.class).get();
        //THEN
        assertAll(
                () -> assertNull(deletedBook1.getId()),
                () -> assertNull(deletedBook2.getId()),
                () -> assertNull(deletedBook3.getId()),
                () -> assertNull(deletedBook1.getTitle()),
                () -> assertNull(deletedBook2.getTitle()),
                () -> assertNull(deletedBook3.getTitle()),
                () -> assertNull(deletedBook1.getPublishedAt()),
                () -> assertNull(deletedBook2.getPublishedAt()),
                () -> assertNull(deletedBook3.getPublishedAt()),
                () -> assertThrows(NoSuchElementException.class, () -> ormManager.findById(savedPublisher.getId(), TestClassBookWithUuidId.class))
        );
    }
}
