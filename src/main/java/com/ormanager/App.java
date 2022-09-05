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
    public static void main(String[] args) throws SQLException {
        LOGGER.info("Welcome to our ORManager impl!");

        initializeEntitiesAndRelations();
    }

    private static void initializeEntitiesAndRelations() throws SQLException {
        var ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];

        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);

        OrmManager<Object> manager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");

        Book harryPotter1 = new Book("Harry Potter and the Java Stone", LocalDate.of(1997, 3, 21));
        Book harryPotter2 = new Book("Harry Potter and the Java of Secrets", LocalDate.of(1998, 4, 13));
        Book harryPotter3 = new Book("Harry Potter and the Prisoner of Kanban", LocalDate.of(1999, 11, 1));
        Book harryPotter4 = new Book("Harry Potter and the Docker of Fire", LocalDate.of(2000, 8, 8));
        Book pirates = new Book("Pirates of Javabeans", LocalDate.of(2008, 5, 18));

        Publisher publisher1 = new Publisher("Java the Hutt");

        Publisher publisher2 = new Publisher("Java Sparrow");

        //publisher1 = (Publisher) manager.save(publisher1);
        harryPotter1 = (Book) manager.save(harryPotter1);
        System.out.println(harryPotter1);
        //harryPotter1.setTitle("Harry Potter and prisoner of kanban");

        System.out.println(manager.merge(harryPotter1));
    }
}