package com.ormanager.orm;

import com.ormanager.orm.test_entities.Book;
import com.ormanager.orm.test_entities.Publisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTest {

    Cache testable = new Cache();

    Publisher publisherOne = new Publisher(1L, "Publisher One");
    Publisher publisherTwo = new Publisher(2L, "Publisher Two");

    Book bookOne;
    Book bookTwo;
    Book bookThree;
    Book bookFour;

    @BeforeEach
    void setUp() {

        bookOne = new Book(1L, "Book One", LocalDate.now().minusYears(10), publisherOne);
        bookTwo = new Book(2L, "Book One", LocalDate.now().minusYears(3), publisherOne);
        bookThree = new Book(3L, "Book One", LocalDate.now().minusYears(12), publisherTwo);
        bookFour = new Book(4L, "Book One", LocalDate.now().minusYears(1), publisherTwo);

        publisherOne.setBooks(List.of(bookOne, bookTwo));
        publisherTwo.setBooks(List.of(bookThree, bookFour));

        testable.putToCache(publisherOne);
        publisherOne.getBooks().forEach(testable::putToCache);

        testable.putToCache(publisherTwo);
        publisherTwo.getBooks().forEach(testable::putToCache);
    }

    @AfterEach
    void tearDown() {
        testable.clearCache();
    }

    @Test
    @DisplayName("COUNT TEST: Should return correct count of Publishers (2) and Books (4) that are stored in cache.")
    void countTest() {

        // given
        Long publishersCount;
        Long booksCount;

        // when
        publishersCount = testable.count(Publisher.class);
        booksCount = testable.count(Book.class);

        // then
        assertThat(publishersCount).isEqualTo(2);
        assertThat(booksCount).isEqualTo(4);
    }

    @Test
    @DisplayName("PUT TO CACHE: Should perform correct put operation into cache.")
    void putToCacheTest() {

        // given
        Publisher publisher = new Publisher(111L, "Java the Hutt");
        publisher.setBooks(List.of(
                new Book(22L, "The Return of Java", LocalDate.now().minusYears(20), publisher),
                new Book(33L, "The Java Strikes Back", LocalDate.now().minusYears(22), publisher)
        ));

        // when
        testable.putToCache(publisher);
        publisher.getBooks().forEach(testable::putToCache);

        // then
        List<Book> allBooksFromCache = testable.getAllFromCache(Book.class);
        List<Publisher> allPublishersFromCache = testable.getAllFromCache(Publisher.class);

        assertThat(allBooksFromCache)
                .hasSize(6)
                .contains(bookOne, bookTwo, bookThree, bookFour)
                .flatMap((Function<? super Book, ?>) Book::getId)
                .isNotNull()
                .contains(22L, 33L);

        assertThat(allPublishersFromCache)
                .hasSize(3)
                .contains(publisherOne, publisherTwo)
                .flatMap(Publisher::getName)
                .contains("Java the Hutt");
    }

    @Test
    @DisplayName("GET FROM CACHE: Should retrieve appropriate object from cache.")
    void getFromCache() {

        // given
        List<Serializable> idList = new ArrayList<>();
        publisherOne.getBooks().forEach(book -> idList.add(book.getId()));
        publisherTwo.getBooks().forEach(book -> idList.add(book.getId()));

        // when
        List<Serializable> idListFromCache = new ArrayList<>();
        idList.forEach(id -> testable.getFromCache(id, Book.class)
                .ifPresent(book -> idListFromCache.add(book.getId())));

        // then
        assertThat(idListFromCache)
                .hasSize(4)
                .contains(1L, 2L, 3L, 4L)
                .isEqualTo(idList);
    }

    @Test
    @DisplayName("GET ALL FROM CACHE: Should return collections of all stored objects specified by their class.")
    void getAllFromCache() {

        // given
        List<Publisher> allPublishersFromCache;
        List<Book> allBooksFromCache;

        // when
        allPublishersFromCache = testable.getAllFromCache(Publisher.class);
        allBooksFromCache = testable.getAllFromCache(Book.class);

        // then
        assertThat(allPublishersFromCache)
                .hasSize(2)
                .containsOnly(publisherOne, publisherTwo);

        assertThat(allBooksFromCache)
                .hasSize(4)
                .containsOnly(bookOne, bookTwo, bookThree, bookFour);
    }

    @Test
    @DisplayName("DELETE FROM CACHE: Should delete specified object from cache based on its ID and class")
    void deleteFromCache() {

        // given
        Cache cache = testable;

        // when
        cache.deleteFromCache(publisherTwo);

        // then
        assertThat(cache.getAllFromCache(Publisher.class))
                .hasSize(1)
                .doesNotContain(publisherTwo)
                .containsOnly(publisherOne);
    }

    @Test
    @DisplayName("IS RECORD IN CACHE: Should return true if object is stored in cache.")
    void isRecordInCache() {

        // given
        Cache cache = testable;

        // when
        boolean result = cache.isRecordInCache(bookThree.getId(), Book.class);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("CLEAR CACHE: Should remove all mappings from cache.")
    void clearCache() {

        // given
        Cache cache = testable;

        // when
        cache.clearCache();

        // then
        assertThat(cache.getEntrySet())
                .isEmpty();
    }
}