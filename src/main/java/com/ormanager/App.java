package com.ormanager;

import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

@Slf4j(topic = "AppTest")
public class App {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LOGGER.info("Welcome to our ORManager impl!");

        var ormManager = OrmManager.withPropertiesFrom("src/main/resources/application.properties");
        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];

        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
    }
}