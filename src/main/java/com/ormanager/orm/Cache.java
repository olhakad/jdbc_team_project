package com.ormanager.orm;

import com.ormanager.orm.annotation.Id;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

@Slf4j(topic = "CacheLog")
public class Cache {

    private final Map<Class<?>, Map<Serializable, Object>> cacheMap;

    Cache() {
        cacheMap = new HashMap<>();
    }

    Long count(Class<?> clazz) {

        long result = cacheMap.get(clazz).entrySet().size();
        LOGGER.trace("Number of records in cache: {}", result);

        return result;
    }

    void putToCache(Object recordToPut) {

        Serializable recordId = getRecordId(recordToPut);
        Class<?> keyClazz = recordToPut.getClass();

        LOGGER.info("Record to put: {}. Record ID: {}.", recordToPut, recordId);

        cacheMap.computeIfAbsent(keyClazz, k -> new HashMap<>())
                .put(recordId, recordToPut);

        LOGGER.info("Put is successful.");
    }

    <T> Optional<T> getFromCache(Serializable recordId, Class<T> clazz) {

        if (cacheMap.get(clazz) == null) return Optional.empty();

        var retrievedRecord = cacheMap.get(clazz).get(recordId);

        LOGGER.info("Retrieving {} from cache.", retrievedRecord);
        return Optional.ofNullable((T) retrievedRecord);

    }

    <T> List<T> getAllFromCache(Class<?> clazz) {

        var values = cacheMap.get(clazz).values();
        return (List<T>) Arrays.asList(values.toArray());
    }

    void deleteFromCache(Object recordToDelete) {

        Serializable recordId = getRecordId(recordToDelete);
        Class<?> keyClazz = recordToDelete.getClass();

        setObjectIdToNull(recordToDelete);

        cacheMap.get(keyClazz).remove(recordId, recordToDelete);

        if (!isRecordInCache(recordId, keyClazz)) {
            LOGGER.info("{} deleted successfully from cache.", recordToDelete);
        }
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

    void clearCache() {
        cacheMap.clear();
    }

    Set<Map.Entry<Class<?>, Map<Serializable, Object>>> getEntrySet() {
        return cacheMap.entrySet();
    }

    public void printCache() {
        List<Long> collectionOfNumbers = new ArrayList<>();
        cacheMap.keySet()
                .forEach(key -> collectionOfNumbers.add(count(key)));

        long numberOfObjectsInCache = collectionOfNumbers.stream().mapToLong(Long::longValue).sum();
        LOGGER.info("Objects stored in cache ({}): {}", numberOfObjectsInCache, cacheMap);
    }

    public void printByClass(Class<?> clazz) {
        Long numberOfObjectsByClass = count(clazz);
        LOGGER.info("Objects of {} class stored in cache ({}): {}",
                clazz.getSimpleName(), numberOfObjectsByClass, cacheMap.get(clazz).toString());
    }
}
