package com.ormanager;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
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
        Publisher publisher = new Publisher("test Publisher");
        Book book1 = new Book("book example 1", LocalDate.of(1979, 2, 23));
        Book book2 = new Book("book example 2", LocalDate.of(1989, 3, 22));
        Book book3 = new Book("book example 3", LocalDate.of(1999, 4, 21));
        publisher.setBooks(List.of(book1, book2, book3));
        ormManager.save(publisher);
        Long book1Id = ormManager.findById(1L, Book.class).get().getId();
        Long book2Id = ormManager.findById(2L, Book.class).get().getId();
        Long book3Id = ormManager.findById(3L, Book.class).get().getId();
        ormManager.delete(publisher);
        System.out.println(ormManager.findById(book1Id, Book.class).get());
        System.out.println();
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