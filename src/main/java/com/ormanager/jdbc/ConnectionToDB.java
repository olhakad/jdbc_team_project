package com.ormanager.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;

public class ConnectionToDB {

    private static String fileName;

    public static void setFileName(String fileName) {
        ConnectionToDB.fileName = fileName;
    }

    private static HikariConfig config = new HikariConfig(fileName);
    private static HikariDataSource ds = new HikariDataSource(config);

    private ConnectionToDB() {}

    public static java.sql.Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
