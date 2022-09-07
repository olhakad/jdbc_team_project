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
        Publisher publisher = (Publisher) ormManager.save(new Publisher("Robur"));
        Publisher publisher2 = (Publisher) ormManager.save(new Publisher("test pub"));
        Book book1 = new Book("Book test 1 ", LocalDate.now());
        Book book2 = new Book("Book test 2", LocalDate.of(2000, 12, 01));
        Book book3 = new Book("Book test 3", LocalDate.of(2015, 11, 03));
        Book book4 = new Book("Book test 4", LocalDate.now());
        book1.setPublisher(publisher);
        book2.setPublisher(publisher2);
        book3.setPublisher(publisher2);
        book4.setPublisher(publisher);
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX" + book1);
        Book bookOne = (Book) ormManager.save(book1);
        Book bookTwo = (Book) ormManager.save(book2);
        Book bookThree = (Book) ormManager.save(book3);
        Book bookFour = (Book) ormManager.save(book4);
        System.out.println("------------ BOOKS ------------");
        ormManager.findById(bookOne.getId(), Book.class).get();//should be robur
        ormManager.findById(bookTwo.getId(), Book.class).get();//should be test pub
        ormManager.findById(bookThree.getId(), Book.class).get();//should be test pub
        ormManager.findById(bookFour.getId(), Book.class).get();//should be robur
        System.out.println("------------ PUBLISHERS ------------");
        ormManager.findById(publisher.getId(), Publisher.class).get();//should have book 1 and 4;
        ormManager.findById(publisher2.getId(), Publisher.class).get();//should have book 2 and 3;
        System.out.println("------------------------");
        ormManager.findAllAsStream(Publisher.class).forEach(System.out::println);
        ormManager.findAllAsStream(Book.class).forEach(System.out::println);


    }
}