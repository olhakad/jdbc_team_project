package com.ormanager.orm;

import com.ormanager.App;
import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.OneToMany;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        System.out.println(retrievedRecord + "=============================");
        String childKeyString = "";
        Class<?> childKey=null;
        for (Field field : retrievedRecord.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class) && Collection.class.isAssignableFrom(field.getType())) {
                childKeyString = field.getGenericType().getTypeName();
                childKeyString = childKeyString.substring(childKeyString.indexOf('<') + 1, childKeyString.indexOf('>'));
                try {
                    childKey = Class.forName(childKeyString);
                    System.out.println(childKey);
                } catch (ClassNotFoundException e) {
                    LOGGER.trace(e.getMessage() + "dark magic");
                }
                System.out.println(cacheMap.keySet());
                System.out.println(childKeyString);
                Collection<Object> values = cacheMap.get(childKey).values();
                values = values.stream().filter(value -> {
                    Optional<Field> any = Arrays.stream(value.getClass().getDeclaredFields()).filter(field1 -> field1.isAnnotationPresent(ManyToOne.class)).findAny();
                    Field parentField = any.get();
                    parentField.setAccessible(true);
                    try {
                        Serializable id = OrmManagerUtil.getId(parentField.get(value));
                        return id == recordId;
                    } catch (IllegalAccessException e) {
                        LOGGER.error(e.getMessage() + "Getting ID for parent");
                    }
                    return false;
                }).toList();
                try {
                    field.set(retrievedRecord,values);
                } catch (IllegalAccessException e) {
                    LOGGER.error(e.getMessage() + "Assigning list of children to parent");
                }
            }
        }

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
