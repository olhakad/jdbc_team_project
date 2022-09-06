//package com.ormanager.orm;
//
//import com.ormanager.client.entity.Book;
//import com.ormanager.client.entity.Publisher;
//import com.ormanager.jdbc.DataSource;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.sql.*;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@Slf4j
//class OrmBookManagerTest {
//    @InjectMocks
//    private OrmManager underTestOrmManager = OrmManager.getConnection();
//
//    OrmBookManagerTest() throws SQLException {
//    }
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        underTestOrmManager = mock(OrmManager.class);
//    }
//
//    @Test
//    void persistTest_ShouldSaveBook() throws IllegalAccessException, SQLException {
//        //Given
//        Book book = new Book(1L, "test", LocalDate.now());
//
//        //When
//        underTestOrmManager.persist(book);
//
//        //Then
//        verify(underTestOrmManager, atLeastOnce()).persist(book);
//    }
//
//    @Test
//    void saveTest_ShouldSaveAndReturnBook() throws IllegalAccessException, SQLException {
//        //Given
//        Book book = new Book(1L, "test", LocalDate.now());
//
//        //When
//        when(underTestOrmManager.save(book)).thenReturn(book);
//        underTestOrmManager.save(book);
//
//        //Then
//        verify(underTestOrmManager, atLeastOnce()).save(book);
//    }
//
//    @Test
//    void findByIdTest_ShouldReturnBookById() {
//        //Given
//        Book book = new Book(1L, "test", LocalDate.now());
//
//        //When
//        when(underTestOrmManager.findById(book.getId(), Book.class)).thenReturn(Optional.of(book));
//        var result = underTestOrmManager.findById(book.getId(), Book.class).orElseThrow();
//
//        //Then
//        verify(underTestOrmManager, atLeastOnce()).findById(book.getId(), Book.class);
//        assertEquals(result, book);
//    }
//
//    @Test
//    void findAllTest_ShouldReturnListOfBooks() throws SQLException {
//        //Given
//        given(underTestOrmManager.findAll(Book.class)).willReturn(new ArrayList<>());
//
//        //When
//        var expected = underTestOrmManager.findAll(Book.class);
//
//        //Then
//        verify(underTestOrmManager, atLeastOnce()).findAll(Book.class);
//    }
//
//}