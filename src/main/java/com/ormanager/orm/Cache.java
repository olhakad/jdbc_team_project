package com.ormanager.orm;

import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.OneToMany;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@Slf4j(topic = "CacheLog")
class Cache {

    private final Map<Class<?>, Map<Serializable, Object>> cacheMap;

    Cache() {
        cacheMap = new HashMap<>();
    }

    void putToCache(Object recordToPut) {

        Serializable recordId = getRecordId(recordToPut);
        Class<?> keyClazz = recordToPut.getClass();

        LOGGER.info("Record to put: {}. Record ID: {}. Class: {}", recordToPut, recordId, keyClazz);

        cacheMap.computeIfAbsent(keyClazz, k -> new HashMap<>())
                .put(recordId, recordToPut);

        LOGGER.info("Put is successful.");
    }

    <T> Optional<T> getFromCache(Serializable recordId, Class<T> clazz) {

        var retrievedRecord = cacheMap.get(clazz).get(recordId);

        LOGGER.info("Retrieving {} from cache.", retrievedRecord);
        return Optional.ofNullable((T) retrievedRecord);
    }

    List<Object> getAllFromCache(Class<?> clazz) {

        var values = cacheMap.get(clazz).values();
        return Arrays.asList(values.toArray());
    }

    void deleteFromCache(Object recordToDelete) {

        Serializable recordId = getRecordId(recordToDelete);
        Class<?> keyClazz = recordToDelete.getClass();

        setObjectIdToNull(recordToDelete);

        cacheMap.get(keyClazz).remove(recordId, recordToDelete);
    }

    boolean isRecordInCache(Serializable recordId, Class<?> clazz) {

        return Optional.ofNullable(cacheMap.get(clazz))
                .map(m -> m.containsKey(recordId))
                .orElse(false);
    }

    private void setObjectIdToNull(Object object) {

        OrmManagerUtil.getIdField(object).ifPresent(field -> {
            field.setAccessible(true);
            try {
                field.set(object, null);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), "When setting id field to null");
            }
        });

        LOGGER.info("{}'s id set to null", object.getClass().getSimpleName());
    }

    private Serializable getRecordId(Object t) {
        Optional<Field> optionalId = Arrays.stream(t.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst();

        if (optionalId.isEmpty()) return null;

        optionalId.get().setAccessible(true);
        try {
            return (Serializable) optionalId.get().get(t);
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage(), "When trying to get record ID.");
            return null;
        }
    }

}
