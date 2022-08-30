package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OrmBookManagerTest {
    @InjectMocks
    private OrmManager<Book> underTestOrmManager;
    private Book bookObject;

    @BeforeEach
    void setUp() {
        try {
            MockitoAnnotations.openMocks(this);
            underTestOrmManager = mock(OrmManager.class);
            bookObject = Book.class.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            LOGGER.info("Exception logger info {}", String.valueOf(e));
        }
    }

    @Test
    void persistTest() throws IllegalAccessException, SQLException {
        //Given
        Book book = new Book(1L, "test", LocalDate.now());

        //When
        underTestOrmManager.persist(book);

        //Then
        verify(underTestOrmManager, atLeastOnce()).persist(book);
    }

    @Test
    void saveTest() throws IllegalAccessException, SQLException {
        //Given
        Book book = new Book(1L, "test", LocalDate.now());

        //When
        when(underTestOrmManager.save(book)).thenReturn(book);
        underTestOrmManager.save(book);

        //Then
        verify(underTestOrmManager, atLeastOnce()).save(book);
    }

    @Test
    void findByIdTest() {
        //Given
        Book book = new Book(1L, "test", LocalDate.now());

        //When
        when(underTestOrmManager.findById(book.getId(), Book.class)).thenReturn(Optional.of(book));
        var result = underTestOrmManager.findById(book.getId(), Book.class).orElseThrow();

        //Then
        verify(underTestOrmManager, atLeastOnce()).findById(book.getId(), Book.class);
        assertEquals(result, book);
    }

    @Test
    void findAllTest() throws SQLException {
        //Given
        given(underTestOrmManager.findAll(Book.class)).willReturn(new ArrayList<>());

        //When
        var expected = underTestOrmManager.findAll(Book.class);

        //Then
        verify(underTestOrmManager, atLeastOnce()).findAll(Book.class);
    }
}