package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.time.LocalDate;

@Slf4j
public class App {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        LOGGER.info("Welcome to our ORManager impl!");
        OrmManager<Book> bookOrmManager = OrmManager.getConnection();
        bookOrmManager.persist(new Book("Book 1", LocalDate.of(1998, 06,18)));
        OrmManager<Publisher> publisherOrmManager = OrmManager.getConnection();
        publisherOrmManager.persist(new Publisher("Robur"));
    }
}
