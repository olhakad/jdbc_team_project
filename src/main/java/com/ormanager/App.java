package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.time.LocalDate;

@Slf4j(topic = "AppTest")
public class App {
    public static void main(String[] args) throws SQLException, NoSuchFieldException, IllegalAccessException {
        LOGGER.info("Welcome to our ORManager impl!");

        initializeEntitiesAndRelations();

        var ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");

        Publisher publisher = new Publisher("testPub");
        Publisher publisherFromDb = (Publisher) ormManager.save(publisher);

        Book book = new Book("testBook", LocalDate.now());
        book.setPublisher(publisherFromDb);
        Book bookFromDb = (Book) ormManager.save(book);
        publisherFromDb.getBooks().add(bookFromDb);

        ormManager.findById(publisherFromDb.getId(), Publisher.class);

        var allPublishers = ormManager.findAll(Publisher.class);
        System.out.println(allPublishers);

        var allBooks = ormManager.findAll(Book.class);
        System.out.println(allBooks);

        System.out.println("-----------");

        publisherFromDb.setName("NEW NAME");
        bookFromDb.setTitle("NEW TITLE OF BOOK");

        ormManager.save(publisherFromDb);
        ormManager.save(bookFromDb);

        var allUpdatedPublisher = ormManager.findAll(Publisher.class);
        System.out.println(allUpdatedPublisher);

        var allUpdatedBooks = ormManager.findAll(Book.class);
        System.out.println(allUpdatedBooks);
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