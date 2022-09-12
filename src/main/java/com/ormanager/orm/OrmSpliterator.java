package com.ormanager.orm;

import com.ormanager.orm.mapper.ObjectMapper;
import lombok.SneakyThrows;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

public class OrmSpliterator<T> implements Spliterator<T> {

    PreparedStatement preparedStatement;
    Class<T> cls;
    Cache ormCache;

    ResultSet resultSet;
    @SneakyThrows
    public OrmSpliterator(PreparedStatement preparedStatement, Class<T> cls, Cache ormCache) {
        this.preparedStatement = preparedStatement;
        this.cls = cls;
        this.ormCache = ormCache;
        resultSet = preparedStatement.executeQuery();
    }

    @SneakyThrows
    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if(!resultSet.next()){
            resultSet.close();
            return false;
        } else {
            resultSet.next();
            Long id = 0L;
            id = resultSet.getLong(OrmManagerUtil.getIdFieldName(cls));
            action.accept(
                    ormCache.getFromCache(id, cls)
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
                            ).get());

            return true;
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        throw new RuntimeException("trySplit command not supported");
    }

    @Override
    public long estimateSize() {
        return 0;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
