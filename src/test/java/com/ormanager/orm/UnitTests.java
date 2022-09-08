package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.jdbc.ConnectionToDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToList;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class UnitTests {

    private static OrmManager ormManager;

    @BeforeAll
    static void setUp() throws SQLException, NoSuchFieldException {
        ormManager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];
        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
    }

    @Test
    void save_ShouldReturnAutoGeneratedIdOfPublisherFromDatabase() throws SQLException, IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("test");
        Long expectedId;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Publishers WHERE id = (SELECT MAX(id) from Publishers);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        }

        //THEN
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    void save_ShouldReturnAutoGeneratedIdOfBookFromDatabase() throws SQLException, IllegalAccessException {
        //GIVEN
        Book book = new Book("test", LocalDate.now());

        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        Long expectedId;

        //WHEN
        ormManager.save(book);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Books WHERE id = (SELECT MAX(id) from Books);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        }

        //THEN
        assertEquals(expectedId, book.getId());
    }

    @Test
    void deleteTest() throws SQLException, IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("deleteTest");

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();
        var deletedValue = ormManager.delete(id);

        //THEN
        assertFalse(deletedValue);
    }

    @Test
    void findById_ShouldReturnPublisherFromDatabaseByGivenId() throws SQLException, IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("test");
        Long expectedId = 0L;

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Publishers WHERE id = " + id + ";")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(publisher, ormManager.findById(publisher.getId(), Publisher.class).orElseThrow());
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    void findById_ShouldReturnBookFromDatabaseByGivenId() throws SQLException, IllegalAccessException {
        //GIVEN
        Book book = new Book("test", LocalDate.now());
        Long expectedId = 0L;

        //WHEN
        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        ormManager.save(book);

        var id = book.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Books WHERE id = " + id + ";")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(book, ormManager.findById(book.getId(), Publisher.class).orElseThrow());
        assertEquals(expectedId, book.getId());
    }

    @Test
    void findByIdPublisherNullValue_ShouldReturnNoSuchElementException() {
        //GIVEN
        Publisher publisher = new Publisher();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(publisher.getId(), Publisher.class));
    }

    @Test
    void findByIdBookNullValue_ShouldReturnNoSuchElementException() {
        //GIVEN
        Book book = new Book();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(book.getId(), Book.class));
    }

    @Test
    void findAllPublishersTest() throws SQLException, IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("saveTestPublisher");
        List<Publisher> publishers;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Publishers;");
            publishers = mapperToList(resultSet, Publisher.class);
        }
        var findAllList = ormManager.findAll(Publisher.class);

        //THEN
        assertTrue(publishers.size() > 0);
        assertEquals(findAllList.size(), publishers.size());
    }

    @Test
    void findAllBooksTest() throws SQLException, IllegalAccessException {
        //GIVEN
        Book book = new Book("test", LocalDate.now());
        List<Book> books;

        //WHEN
        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        ormManager.save(book);

        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Books;");
            books = mapperToList(resultSet, Book.class);
        }
        var findAllList = ormManager.findAll(Book.class);

        //THEN
        assertTrue(books.size() > 0);
        assertEquals(findAllList.size(), books.size());
    }
}
