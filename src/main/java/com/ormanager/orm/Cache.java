package com.ormanager.orm;

import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.OneToMany;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

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
        Optional<T> retrievedRecord1;
        assignChildrenToParentCollection(recordId, retrievedRecord);
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

    /**
     * Inserts collection of children to parent's filed
     *
     * @param recordId        parent's id
     * @param retrievedRecord parent to be retrieved
     */
    private void assignChildrenToParentCollection(Serializable recordId, Object retrievedRecord) {
        for (Field field : retrievedRecord.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class) && Collection.class.isAssignableFrom(field.getType())) {
                Class<?> childKey = getChildClass(field);
                Collection<Object> values = getChildren(recordId, childKey);
                try {
                    field.set(retrievedRecord, values);
                } catch (IllegalAccessException e) {
                    LOGGER.error(e.getMessage() + "Assigning list of children to parent");
                }
            }
        }
    }

    /**
     * Loads generic type of class from collection
     *
     * @param field is a parent's field with children
     * @return class of children
     */
    private static Class<?> getChildClass(Field field) {
        String childKeyString = field.getGenericType().getTypeName();
        childKeyString = childKeyString.substring(childKeyString.indexOf('<') + 1, childKeyString.indexOf('>'));
        Class<?> childKey = null;
        try {
            childKey = Class.forName(childKeyString);
        } catch (ClassNotFoundException e) {
            LOGGER.trace(e.getMessage() + "dark magic");
        }
        return childKey;
    }

    /**
     * Looks for children is cache
     *
     * @param recordId is a parent's id
     * @param childKey is a key to go through map
     * @return collection of children
     */
    private Collection<Object> getChildren(Serializable recordId, Class<?> childKey) {
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
        return values;
    }

}
