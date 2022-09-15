package com.ormanager.orm;

import com.ormanager.orm.mapper.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;

import static com.ormanager.orm.OrmManagerUtil.getIdFieldName;
import static com.ormanager.orm.OrmManagerUtil.isIdFieldNumericType;

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

    private T getEntity(ResultSet resultSet) throws NoSuchFieldException, SQLException {
        var id = isIdFieldNumericType(cls) ?
                resultSet.getLong(getIdFieldName(cls)) : UUID.fromString(resultSet.getString(getIdFieldName(cls)));
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
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
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
