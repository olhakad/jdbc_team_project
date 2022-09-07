package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

import static com.ormanager.orm.OrmManager.withDataSource;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OrmBookManagerTest {
    @Mock
    DataSource underTestDataSource;
    @Mock
    Connection underTestConnection;
    @Mock
    Statement underTestStatement;
    @Mock
    PreparedStatement underTestPreparedStatement;
    @Mock
    ResultSet underTestResultSet;
    @InjectMocks
    OrmManager underTestOrmManager;

    @BeforeEach
    void setUp() throws SQLException {
        //Given
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = withDataSource(underTestDataSource);
        when(underTestConnection.createStatement()).thenReturn(underTestStatement);
    }

    @Test
    public void updateTest() throws Exception {
        //When
        when(underTestConnection.createStatement().executeUpdate(anyString())).thenReturn(1);
        var updatedBook = underTestOrmManager.update(any(Book.class));

        //Then
        assertNotNull(updatedBook);
        verify(underTestConnection.createStatement(), atLeastOnce());
    }

    @Test
    void saveTest() throws SQLException, IllegalAccessException {
        //When
        when(underTestConnection.createStatement().executeUpdate(anyString())).thenReturn(1);
        when(underTestOrmManager.save(any(Book.class))).thenReturn(any(Book.class));

        //Then
        verify(underTestDataSource.getConnection().prepareStatement(any(Book.class).toString()).executeUpdate(), atLeastOnce());
    }

    @Test
    void persistTest() throws SQLException, IllegalAccessException {
        Publisher b = new Publisher("test");
        underTestOrmManager.persist(b);

        //Then
        verify(underTestDataSource.getConnection().prepareStatement(any(Book.class).toString()).executeUpdate(), atLeastOnce());
    }

    @Test
    void findAllTest() throws SQLException {
        //When
        when(underTestPreparedStatement.executeQuery()).thenReturn(underTestResultSet);
        when(underTestOrmManager.findAll(Book.class)).thenReturn(new ArrayList<>());

        //Then
        verify(underTestDataSource.getConnection().prepareStatement(anyString()), atLeastOnce());
    }
}
