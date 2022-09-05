package com.ormanager.orm;

import com.ormanager.orm.annotation.Id;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j(topic = "CacheLog")
class Cache<T> {

    private final Map<Class<?>, Map<Serializable, T>> cacheMap;

    Cache() {
        cacheMap = new HashMap<>();
    }

    void putToCache(T recordToPut) throws IllegalAccessException {

        Serializable recordId = getRecordId(recordToPut);
        Class<?> keyClazz = recordToPut.getClass();

        LOGGER.info("Record to put: {}. Record ID: {}. Class: {}", recordToPut, recordId, keyClazz);

        if (cacheMap.containsKey(keyClazz)) {
            cacheMap.get(keyClazz).put(recordId, recordToPut);
            LOGGER.info("Cache already has such key. Put is proceeded.");
            return;
        }

        Map<Serializable, T> innerCacheMap = new HashMap<>();
        innerCacheMap.put(recordId, recordToPut);

        LOGGER.info("Initializing new key. Put is proceeded.");
        cacheMap.putIfAbsent(keyClazz, innerCacheMap);
    }

    <T> T get(Serializable recordId, Class<T> clazz) {

        T retrievedRecord = (T) cacheMap.get(clazz).get(recordId);
        LOGGER.info("Retrieving {} from cache.", retrievedRecord);

        return retrievedRecord;
    }

    boolean deleteFromCache(T recordToDelete) throws IllegalAccessException {

        Serializable recordId = getRecordId(recordToDelete);
        Class<?> keyClazz = recordToDelete.getClass();

        return cacheMap.get(keyClazz).remove(recordId, recordToDelete);
    }

    <T> boolean isRecordInCache(Serializable recordId, Class<T> clazz) {

        return cacheMap.get(clazz).containsKey(recordId);
    }

    private Serializable getRecordId(T t) throws IllegalAccessException {

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
