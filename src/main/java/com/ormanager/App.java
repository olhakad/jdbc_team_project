package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;

@Slf4j
public class App {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LOGGER.info("Welcome to our ORManager impl!");
        OrmManager<Book> bookOrmManager = OrmManager.getConnection();
        System.out.println(bookOrmManager.save(new Book("test book", LocalDate.of(1999, 9, 9))));
    }
}
