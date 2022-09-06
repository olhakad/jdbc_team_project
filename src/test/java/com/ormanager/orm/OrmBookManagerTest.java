package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.jdbc.ConnectionToDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OrmBookManagerTest {
    @Mock
    private DataSource underTestDataSource;
    @Mock
    private ConnectionToDB connectionToDB;
    @Mock
    private Connection underTestConnection;
    @Mock
    private Statement underTestStatement;
    @Mock
    private PreparedStatement underTestPreparedStatement;
    @Mock
    private ResultSet underTestResultSet;

    private OrmManager underTestOrmManager;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = OrmManager.withDataSource(underTestDataSource);
    }

    @Test
    void saveTest() throws SQLException, IllegalAccessException {

        //When
        when(underTestOrmManager.save(any(Book.class))).thenReturn(any(Book.class));

        //Then
        verify(underTestDataSource.getConnection().prepareStatement(any(Book.class).toString()).executeUpdate(), atLeastOnce());
    }

    @Test
    void persistTest() throws SQLException, IllegalAccessException {

        //When
       // doNothing().when(underTestOrmManager.persist(any(Book.class)));

        //Then
        verify(underTestDataSource.getConnection().prepareStatement(any(Book.class).toString()).executeUpdate(), atLeastOnce());
    }

    @Test
    void findAllTest_ShouldReturnListOfBooks() throws SQLException {
        when(underTestOrmManager.findAll(Book.class)).thenReturn(new ArrayList<>());

        verify(underTestDataSource.getConnection().prepareStatement(anyString()),atLeastOnce());
    }
}
