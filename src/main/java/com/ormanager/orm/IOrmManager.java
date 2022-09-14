package com.ormanager.orm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface IOrmManager {

    void register(Class<?>... entityClasses) throws SQLException, NoSuchFieldException;

    Object save(Object o);

    void persist(Object o) throws SQLException, IllegalAccessException;

    <T> Optional<T> findById(Serializable id, Class<T> cls);

    <T> List<T> findAll(Class<T> cls);

    <T> IterableORM<T> findAllAsIterable(Class<T> cls)  throws SQLException;

    <T> Stream<T> findAllAsStream(Class<T> cls) throws SQLException;

    boolean merge(Object o);

    Object update(Object o);

    boolean delete(Object o);

    void dropEntity(Class<?> clazz);

    void createRelationships(Class<?>... entityClasses) throws SQLException, NoSuchFieldException;

    Cache getOrmCache();
}
