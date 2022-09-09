package com.ormanager.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionToDB {
    private static String fileName;

    public static void setFileName(String fileName) {
        ConnectionToDB.fileName = fileName;
    }

    private ConnectionToDB() {
    }

    public static Connection getConnection() throws SQLException {
        HikariConfig config = new HikariConfig(fileName);
        HikariDataSource ds = new HikariDataSource(config);
        return ds.getConnection();
    }
}
