package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class OrmManagerDeleteTest {

    private IOrmManager ormManager;

    @BeforeEach
    void setUp() throws SQLException, NoSuchFieldException {
        ormManager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
        ormManager.dropEntity(Book.class);
        ormManager.dropEntity(Publisher.class);

        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];
        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
    }

    @Test
    void whenDeletingPublisher_ShouldDeletePublisherAndBooksAndSetIdToNull() {

        // given
        Publisher publisher = new Publisher("testPub");
        Book book = new Book("testBook", LocalDate.now());
        publisher.getBooks().add(book);
        ormManager.save(publisher);

        // when
        ormManager.delete(publisher);

        // then
        assertNull(publisher.getId());
        assertNull(book.getId());
        assertFalse(ormManager.getOrmCache().isRecordInCache(publisher.getId(), Publisher.class));
        assertFalse(ormManager.getOrmCache().isRecordInCache(book.getId(), Book.class));
    }

    @Test
    void whenDeletingBook_ShouldDeleteBookAndSetIdToNull() {

        // given
        Book book = (Book) ormManager.save(new Book("testBook", LocalDate.now()));
        Long id = book.getId();

        // when
        ormManager.delete(book);

        // then
        assertNull(book.getId());
        assertFalse(ormManager.getOrmCache().isRecordInCache(id, book.getClass()));
    }

    @Test
    void whenDeletingPublisherWithAssignedBooks_ShouldDeleteAssignedBooksAndPublisher() {

        // given
        Publisher publisher = new Publisher("test Publisher");
        Book book1 = new Book("book example 1", LocalDate.of(1979, 2, 23));
        Book book2 = new Book("book example 2", LocalDate.of(1989, 3, 22));
        Book book3 = new Book("book example 3", LocalDate.of(1999, 4, 21));
        publisher.setBooks(List.of(book1, book2, book3));
        ormManager.save(publisher);

        // when
        boolean deleteResult = ormManager.delete(publisher);

        // then
        assertAll(
                () -> assertTrue(deleteResult),
                () -> assertNull(book1.getId()),
                () -> assertNull(book2.getId()),
                () -> assertNull(book3.getId()),
                () -> assertNotNull(book1.getTitle()),
                () -> assertNotNull(book2.getTitle()),
                () -> assertNotNull(book3.getTitle()),
                () -> assertNotNull(book1.getPublishedAt()),
                () -> assertNotNull(book2.getPublishedAt()),
                () -> assertNotNull(book3.getPublishedAt()),
                () -> assertNull(publisher.getId()),
                () -> assertFalse(ormManager.getOrmCache().isRecordInCache(publisher.getId(), Publisher.class)),
                () -> assertFalse(ormManager.getOrmCache().isRecordInCache(book1.getId(), Book.class)),
                () -> assertFalse(ormManager.getOrmCache().isRecordInCache(book2.getId(), Book.class)),
                () -> assertFalse(ormManager.getOrmCache().isRecordInCache(book3.getId(), Book.class)),
                () -> assertFalse(ormManager.isRecordInDataBase(publisher)),
                () -> assertFalse(ormManager.isRecordInDataBase(book1)),
                () -> assertFalse(ormManager.isRecordInDataBase(book2)),
                () -> assertFalse(ormManager.isRecordInDataBase(book3)),
                () -> assertThrows(NoSuchElementException.class, () -> ormManager.findById(publisher.getId(), Book.class))
        );
    }
}