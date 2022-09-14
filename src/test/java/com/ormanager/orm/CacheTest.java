package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

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
        List<Object> allPublishersFromCache = testable.getAllFromCache(Publisher.class);

        assertThat(allBooksFromCache)
                .hasSize(6)
                .contains(bookOne, bookTwo, bookThree, bookFour)
                .flatMap((Function<? super Book, ?>) Book::getId)
                .isNotNull()
                .contains(22L, 33L);

        assertThat(allPublishersFromCache)
                .hasSize(3)
                .contains(publisherOne, publisherTwo);
    }

    @Test
    void getFromCache() {

        // given
        // when
        // then
    }

    @Test
    void getAllFromCache() {

        // given
        // when
        // then
    }

    @Test
    void deleteFromCache() {

        // given
        // when
        // then
    }

    @Test
    void isRecordInCache() {

        // given
        // when
        // then
    }

    @Test
    void clearCache() {

        // given
        // when
        // then
    }
}