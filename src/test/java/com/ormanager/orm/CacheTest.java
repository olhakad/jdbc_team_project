package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

        publisherOne.setBooks(new ArrayList<>(List.of(bookOne, bookTwo)));
        publisherTwo.setBooks(new ArrayList<>(List.of(bookThree, bookFour)));

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
    @DisplayName("COUNT: Should return correct count of Publishers (2) and Books (4) that are stored in cache.")
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
    void putToCache() {

        // given
        // when
        // then
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