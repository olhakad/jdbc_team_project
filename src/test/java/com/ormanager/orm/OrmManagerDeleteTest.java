package com.ormanager.orm;

import com.ormanager.orm.test_entities.Book;
import com.ormanager.orm.test_entities.Publisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class OrmManagerDeleteTest {

    private IOrmManager testable;

    @BeforeEach
    void setUp() throws SQLException, NoSuchFieldException {
        testable = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");

        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];
        entityClassesAsSet.toArray(entityClassesAsArray);
        testable.register(entityClassesAsArray);
        testable.createRelationships(entityClassesAsArray);
    }

    @AfterEach
    void tearDown() {
        testable.dropEntity(Book.class);
        testable.dropEntity(Publisher.class);
    }

    @Test
    void givenSavedBook_whenIsDeleted_shouldBeRemovedFromDbAndCache() {

        // given
        Book book = new Book("testBook", LocalDate.now());
        testable.save(book);
        Long id = book.getId();

        // when
        testable.delete(book);

        // then
        assertNull(book.getId());
        assertFalse(testable.getOrmCache().isRecordInCache(id, book.getClass()));
    }

    @Test
    void givenSavedPublisherWithBook_whenBookIsDeleted_shouldBeRemovedFromDbAndCache() {

        // given
        Publisher publisher = new Publisher("test Publisher");
        Book book = new Book("testBook", LocalDate.now());
        publisher.setBooks(List.of(book));

        testable.save(publisher);

        // when
        testable.delete(book);

        // then
        assertAll(
                () -> assertNull(book.getId()),
                () -> assertNotNull(book.getPublisher().getId()),
                () -> assertNotNull(publisher.getId()),
                () -> assertNull(publisher.getBooks().get(0).getId()),
                () -> assertFalse(testable.isRecordInDataBase(book)),
                () -> assertTrue(testable.isRecordInDataBase(publisher)),
                () -> assertFalse(testable.getOrmCache().isRecordInCache(book.getId(), book.getClass())),
                () -> assertTrue(testable.getOrmCache().isRecordInCache(publisher.getId(), publisher.getClass()))
        );
    }

    @Test
    void givenSavedPublisherWithBooks_whenPublisherIsDeleted_shouldBeRemovedWithAllAssignedBooksFromDbAndCache() {

        // given
        Publisher publisher = new Publisher("test Publisher");
        Book book1 = new Book("book example 1", LocalDate.of(1979, 2, 23));
        Book book2 = new Book("book example 2", LocalDate.of(1989, 3, 22));
        Book book3 = new Book("book example 3", LocalDate.of(1999, 4, 21));
        publisher.setBooks(List.of(book1, book2, book3));
        testable.save(publisher);

        // when
        boolean deleteResult = testable.delete(publisher);

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
                () -> assertFalse(testable.getOrmCache().isRecordInCache(publisher.getId(), Publisher.class)),
                () -> assertFalse(testable.getOrmCache().isRecordInCache(book1.getId(), Book.class)),
                () -> assertFalse(testable.getOrmCache().isRecordInCache(book2.getId(), Book.class)),
                () -> assertFalse(testable.getOrmCache().isRecordInCache(book3.getId(), Book.class)),
                () -> assertFalse(testable.isRecordInDataBase(publisher)),
                () -> assertFalse(testable.isRecordInDataBase(book1)),
                () -> assertFalse(testable.isRecordInDataBase(book2)),
                () -> assertFalse(testable.isRecordInDataBase(book3)),
                () -> assertThrows(NoSuchElementException.class, () -> testable.findById(publisher.getId(), Book.class))
        );
    }
}