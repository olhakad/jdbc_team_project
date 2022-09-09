package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
import com.ormanager.orm.OrmManagerUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j(topic = "AppTest")
public class App {
    public static void main(String[] args) throws SQLException, NoSuchFieldException, IllegalAccessException {
        LOGGER.info("Welcome to our ORManager impl!");

        initializeEntitiesAndRelations();

        var ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");

        Publisher publisher = new Publisher("testPub");
        publisher.getBooks().add(new Book("Harry", LocalDate.now()));
        publisher.getBooks().add(new Book("Robur", LocalDate.now()));

    /*    List<Object> children = OrmManagerUtil.getChildren(publisher);
        System.out.println("CHILDREN "+children);*/

        Publisher publisherFromDb = (Publisher) ormManager.save(publisher);
        Publisher publisherWithoutBooks = (Publisher) ormManager.save(new Publisher("Publisher without books"));
        Book bookWithoutPublisher = (Book) ormManager.save(new Book("book without publisher", LocalDate.now()));
        bookWithoutPublisher.setPublisher(publisherWithoutBooks);
        
        Book bookFromDb = ormManager.findById(11L, Book.class).get();
        System.out.println("----------------------------------------");
        ormManager.findAllAsStream(Book.class).forEach(System.out::println);
        ormManager.findAllAsStream(Publisher.class).forEach(System.out::println);
        System.out.println("----------------------------------------");
        ormManager.delete(publisherFromDb);
    }

    private static void initializeEntitiesAndRelations() throws SQLException, NoSuchFieldException {
        var ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];

        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
    }
}