package com.ormanager.orm;

import com.ormanager.orm.mapper.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

@Slf4j(topic = "OrmSpliterator")
public class OrmSpliterator<T> implements Spliterator<T> {

    Class<T> cls;
    Cache ormCache;
    final ResultSet resultSet;
    int counter = 0;

    public OrmSpliterator(final ResultSet resultSet, Class<T> cls, Cache ormCache) {
        this.cls = cls;
        this.ormCache = ormCache;
        this.resultSet = resultSet;
    }

    T getEntity(ResultSet resultSet) {
        Long id = 0L;
        try {
            id = resultSet.getLong(OrmManagerUtil.getIdFieldName(cls));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        return ormCache.getFromCache(id, cls)
                .or(() -> {
                            T resultFromDb = null;
                            try {
                                resultFromDb = cls.getConstructor().newInstance();
                                ObjectMapper.mapperToObject(resultSet, resultFromDb);
                                ormCache.putToCache(resultFromDb);

                            } catch (ReflectiveOperationException e) {
                            }
                            return Optional.ofNullable(resultFromDb);
                        }
                ).get();
    }

    private boolean next() {
        try {
            return resultSet.next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        counter++;
        if (next()) {
            action.accept(getEntity(resultSet));
            return true;
        } else {
            try {
                resultSet.close();
                LOGGER.info("ResultSet closed");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return counter;
    }

    @Override
    public int characteristics() {
        return DISTINCT;
    }
}
