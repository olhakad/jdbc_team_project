package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OrmBookManagerTest {
    @Mock
    private DataSource underTestDataSource;
    @Mock
    private Connection underTestConnection;
    @Mock
    private Statement underTestStatement;
    @Mock
    private ResultSet underTestResultSet;
    private OrmManager<Book> underTestOrmManager;

    @BeforeEach
    void setUp() throws SQLException, ClassNotFoundException {
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = OrmManager.withDataSource(underTestDataSource);
        underTestConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test","root","root");
        underTestStatement = underTestConnection.createStatement();
    }

    @Test
    void findAllTest_ShouldReturnListOfBooks() throws SQLException {
        //Given
        given(underTestStatement.executeQuery("select * from books")).willReturn(underTestResultSet);

        //When
        var expected = underTestOrmManager.findAll(Book.class);

        //Then
        assertTrue(expected.size() > 0);
    }
}