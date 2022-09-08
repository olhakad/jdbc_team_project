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

    void deleteFromCache(Object recordToDelete) throws IllegalAccessException {

        Serializable recordId = getRecordId(recordToDelete);
        Class<?> keyClazz = recordToDelete.getClass();
        //todo extract methods, will we need them?
//        if (isParent(keyClazz)) {
//            List<Field> oneToManyFields = OrmManagerUtil.getRelationshipFields(keyClazz, OneToMany.class);
//
//            oneToManyFields.stream().flatMap(field -> {
//                        field.setAccessible(true);
//                        Class<?> childKey = getGenericParameterFromField(field);
//                        Collection<Object> values = getChildrenFromCache(recordId, childKey);
//                        return values.stream();
//                    })
//                    .forEach(child -> ormCache.deleteFromCache);
//
//            LOGGER.info("{}'s (id = {}) all children's ids set to null", recordToDelete.getClass().getSimpleName(), recordId);
//        }

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

    /**
     * Inserts collection of children to parent's filed
     *
     * @param recordId        parent's id
     * @param retrievedRecord parent to be retrieved
     */
    private void assignChildrenToParentCollection(Serializable recordId, Object retrievedRecord) {
        var retrievedRecordClass = retrievedRecord.getClass();

        List<Field> oneToManyFields = OrmManagerUtil.getRelationshipFields(retrievedRecordClass, OneToMany.class);

        oneToManyFields.forEach(field -> {
            field.setAccessible(true);

            Class<?> childKey = getGenericParameterFromField(field);
            Collection<Object> values = getChildrenFromCache(recordId, childKey);
            try {
                field.set(retrievedRecord, values);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), "When assigning list of children to parent");
            }
        });
    }

    private static Class<?> getGenericParameterFromField(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    /**
     * Looks for children in cache
     *
     * @param recordId is a parent's id
     * @param childKey is a key to go through map
     * @return collection of children
     */
    private Collection<Object> getChildrenFromCache(Serializable recordId, Class<?> childKey) {

        if (cacheMap.get(childKey).isEmpty()) return Collections.emptyList();

        Collection<Object> values = cacheMap.get(childKey).values();
        values = values.stream().filter(value -> {
            Optional<Field> childCollection = Arrays
                    .stream(value.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(ManyToOne.class))
                    .findAny();

            if (childCollection.isEmpty()) return false;

            Field parentField = childCollection.get();

            parentField.setAccessible(true);
            try {
                Serializable id = OrmManagerUtil.getId(parentField.get(value));
                return id == recordId;
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), "When getting ID for parent");
                return false;
            }
        }).toList();

        return values;
    }

    private boolean isParent(Class<?> keyClazz) {
        return OrmManagerUtil.doesClassHaveGivenRelationship(keyClazz, OneToMany.class);
    }
}
