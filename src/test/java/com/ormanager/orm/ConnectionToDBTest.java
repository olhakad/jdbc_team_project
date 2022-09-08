package com.ormanager.orm;

import java.sql.Connection;
import java.sql.Statement;

import com.ormanager.jdbc.ConnectionToDB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionToDBTest {

    @BeforeAll
    public static void setUp() {
        ConnectionToDB.setFileName("src/test/resources/application_test.properties");
    }

    @Test
    public void whenConnectionEstablished_Expect_connectionNotNull() throws Exception {
        Connection connection = ConnectionToDB.getConnection();
        assertNotNull(connection);
    }

    @Test
    public void whenConnectionEstablishedAndClosed_Expect_connectionIsClosed() throws Exception {
        Connection connection = ConnectionToDB.getConnection();
        connection.close();
        assertTrue(connection.isClosed());
    }

    @Test
    public void whenConnectionsEstablished_Expect_connectionsAreNotNullAndNotEquals() throws Exception {
        Connection connection = ConnectionToDB.getConnection();
        Connection connection2 = ConnectionToDB.getConnection();
        assertNotNull(connection);
        assertNotNull(connection2);
        assertNotEquals(connection, connection2);
    }

    @Test
    public void whenConnectionEstablishedAndQueryExecuted_Expect_resultIsTrue() throws Exception {
        Connection connection = ConnectionToDB.getConnection();
        boolean result = connection.prepareStatement("SELECT 1").execute();
        assertTrue(result);
    }

}
