package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;

@Slf4j(topic = "AppTest")
public class App {
    public static OrmManager ormManager;

    static {
        try {
            ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LOGGER.info("Welcome to our ORManager impl!");


        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];

        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
        Book book = new Book("Book Title", LocalDate.now());
        Publisher publisher = (Publisher) ormManager.save(new Publisher("Jakub"));
        book.setPublisher(ormManager.findById(publisher.getId(), Publisher.class).get());
        System.out.println(ormManager.save(book));
        System.out.println("----------BOOK--------");
        System.out.println(ormManager.findById(book.getId(), Book.class));
        System.out.println("----------PUBLISHER--------");
        System.out.println(ormManager.findById(publisher.getId(), Publisher.class));

    }
}