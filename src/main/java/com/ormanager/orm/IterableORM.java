package com.ormanager.orm;
import java.util.Iterator;
public interface IterableORM<T> extends AutoCloseable, Iterator<T> {

    @Override
    void close() throws Exception;

    @Override
    boolean hasNext();

    @Override
    T next();
}
