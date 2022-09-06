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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private PreparedStatement underTestPreparedStatement;
    @Mock
    private ResultSet underTestResultSet;
    private OrmManager underTestOrmManager;

    @BeforeEach
    void setUp() throws SQLException, ClassNotFoundException {
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = OrmManager.withDataSource(underTestDataSource);
    }

    @Test
    void saveTest() throws SQLException, IllegalAccessException {
        //When
        when(underTestConnection.prepareStatement(any(Book.class).toString()).executeUpdate()).thenReturn(1);

        //Then
        verify(underTestConnection.prepareStatement(any(Book.class).toString()).executeUpdate(), atLeastOnce());
    }

    @Test
    void findAllTest_ShouldReturnListOfBooks() throws SQLException {
        when(underTestConnection.prepareStatement(any(Book.class).toString())).thenReturn(underTestPreparedStatement);

        verify(underTestResultSet).first();
    }
}