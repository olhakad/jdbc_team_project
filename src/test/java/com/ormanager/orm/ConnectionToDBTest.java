package com.ormanager.orm;

import java.sql.Connection;
import java.sql.Statement;

import com.ormanager.jdbc.ConnectionToDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionToDBTest {

    @InjectMocks private ConnectionToDB dbConnection;
    @Mock
    private Connection mockConnection;
    @Mock private Statement mockStatement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMockDBConnection() throws Exception {
        Mockito.when(mockConnection.createStatement()).thenReturn(mockStatement);
        Mockito.when(mockConnection.createStatement().executeUpdate(Mockito.any())).thenReturn(1);
        ConnectionToDB.setFileName("src/main/resources/application.properties");
        int value = dbConnection.getConnection().createStatement().executeUpdate("");
        assertEquals(value, 1);
        Mockito.verify(mockConnection.createStatement(), Mockito.times(1));
    }

}
