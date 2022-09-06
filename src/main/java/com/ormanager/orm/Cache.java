package com.ormanager.orm;

import com.ormanager.App;
import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Id;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "CacheLog")
class Cache {

    private final Map<Class<?>, Map<Serializable, Object>> cacheMap;

    Cache() {
        cacheMap = new HashMap<>();
    }

    void putToCache(Object recordToPut) throws IllegalAccessException {

        Serializable recordId = getRecordId(recordToPut);
        Class<?> keyClazz = recordToPut.getClass();

        LOGGER.info("Record to put: {}. Record ID: {}. Class: {}", recordToPut, recordId, keyClazz);

        if (cacheMap.containsKey(keyClazz)) {
            cacheMap.get(keyClazz).put(recordId, recordToPut);
            LOGGER.info("Cache already has such key. Put is proceeded.");
            return;
        }

        Map<Serializable, Object> innerCacheMap = new HashMap<>();
        innerCacheMap.put(recordId, recordToPut);

        LOGGER.info("Initializing new key. Put is proceeded.");
        cacheMap.putIfAbsent(keyClazz, innerCacheMap);
    }

    <T> Optional<T> getFromCache(Serializable recordId, Class<T> clazz) {
        var retrievedRecord = cacheMap.get(clazz).get(recordId);

//        if (retrievedRecord instanceof Publisher publisher) {
//            List<Book> books = new ArrayList<>();
//            //books.add(App.ormManager.findById(2L, Book.class).get());
//            App.ormManager.findAllAsStream(Book.class)
//                    .map(b -> (Book)b)
//                    //.filter(b -> b.getPublisher().getId()== publisher.getId())
//                    .peek(a -> System.out.println("---------"+a))
//                    .filter(b->b.getPublisher()!=null)
//                    .collect(Collectors.toList());
//            ((Publisher) retrievedRecord).setBooks(books);
//        }
        LOGGER.info("Retrieving {} from cache.", retrievedRecord);

        return Optional.ofNullable((T) retrievedRecord);
    }

    boolean deleteFromCache(Object recordToDelete) throws IllegalAccessException {

        Serializable recordId = getRecordId(recordToDelete);
        Class<?> keyClazz = recordToDelete.getClass();

        return cacheMap.get(keyClazz).remove(recordId, recordToDelete);
    }

    boolean isRecordInCache(Serializable recordId, Class<?> clazz) {
        return Optional.ofNullable(cacheMap.get(clazz))
                .map(m -> m.containsKey(recordId))
                .orElse(false);
    }

    private Serializable getRecordId(Object t) throws IllegalAccessException {

        Optional<Field> optionalId = Arrays.stream(t.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst();
        if (optionalId.isPresent()) {
            optionalId.get().setAccessible(true);
            return (Serializable) optionalId.get().get(t);
        }
        return null;
    }
}
