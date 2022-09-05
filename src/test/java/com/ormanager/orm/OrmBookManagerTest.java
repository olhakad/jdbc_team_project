package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OrmBookManagerTest {
    @Mock
    private DataSource underTestDataSource;
    @Mock
    private Connection underTestConnection;
    @Mock
    private PreparedStatement underTestPreparedStatement;
    @Mock
    private ResultSet underTestResultSet;
    @InjectMocks
    private OrmManager<Book> underTestOrmManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = mock(OrmManager.class);
    }

    @Test
    void findAllTest_ShouldReturnListOfBooks() throws SQLException {
        //Given
        given(underTestConnection.createStatement().executeQuery("select * from books")).willReturn(underTestResultSet);

        //When
        var expected = underTestOrmManager.findAll(Book.class);

        //Then
        assertTrue(expected.size() > 0);
    }
}