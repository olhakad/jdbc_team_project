package com.ormanager.orm;

import com.ormanager.orm.mapper.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

@Slf4j(topic = "OrmSpliterator")
public class OrmSpliterator<T> implements Spliterator<T> {

    private Class<T> cls;
    private Cache ormCache;
    private ResultSet resultSet;
    private int counter = 0;

    public OrmSpliterator(ResultSet resultSet, Class<T> cls, Cache ormCache) {
        this.cls = cls;
        this.ormCache = ormCache;
        this.resultSet = resultSet;
    }

    private T getEntity(ResultSet resultSet) {
        Long id = 0L;
        try {
            id = resultSet.getLong(OrmManagerUtil.getIdFieldName(cls));
        } catch (SQLException | NoSuchFieldException e) {
            LOGGER.warn(e.getMessage());
        }

        return ormCache.getFromCache(id, cls)
                .or(() -> {
                            T resultFromDb = null;
                            try {
                                resultFromDb = cls.getConstructor().newInstance();
                                ObjectMapper.mapperToObject(resultSet, resultFromDb);
                                ormCache.putToCache(resultFromDb);

                            } catch (ReflectiveOperationException e) {
                                LOGGER.warn(e.getMessage());
                            }
                            return Optional.ofNullable(resultFromDb);
                        }
                ).get();
    }

    private boolean next() throws SQLException {
        return resultSet.next();
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        counter++;
        try {
            if (next()) {
                action.accept(getEntity(resultSet));
                return true;
            } else {
                resultSet.close();
                LOGGER.info("ResultSet closed");
            }
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage());
        }
        return false;
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
