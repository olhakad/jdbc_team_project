package com.ormanager;

import com.ormanager.orm.ClassScanner;
import com.ormanager.orm.OrmManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

@Slf4j(topic = "AppTest")
public class App {
    public static void main(String[] args) throws SQLException, NoSuchFieldException {
        LOGGER.info("Welcome to our ORManager impl!");

        initializeEntitiesAndRelations();
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