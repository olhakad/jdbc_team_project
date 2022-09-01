package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class OrmPublisherManagerTest {
    @InjectMocks
    private OrmManager<Publisher> underTestOrmManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTestOrmManager = mock(OrmManager.class);
    }

    @Test
    void persistTest_ShouldSavePublisher() throws IllegalAccessException, SQLException {
        //Given
        Publisher publisher = new Publisher(1L, "test", List.of(new Book(), new Book()));

        //When
        underTestOrmManager.persist(publisher);

        //Then
        verify(underTestOrmManager, atLeastOnce()).persist(publisher);
    }

    @Test
    void saveTest_ShouldSaveAndReturnPublisher() throws IllegalAccessException, SQLException {
        //Given
        Publisher publisher = new Publisher(1L, "test", List.of(new Book(), new Book()));

        //When
        when(underTestOrmManager.save(publisher)).thenReturn(publisher);
        underTestOrmManager.save(publisher);

        //Then
        verify(underTestOrmManager, atLeastOnce()).save(publisher);
    }

    @Test
    void findByIdTest_ShouldReturnPublisherById() {
        //Given
        Publisher publisher = new Publisher(1L, "test", List.of(new Book(), new Book()));

        //When
        when(underTestOrmManager.findById(publisher.getId(), Publisher.class)).thenReturn(Optional.of(publisher));
        var result = underTestOrmManager.findById(publisher.getId(), Publisher.class).orElseThrow();

        //Then
        verify(underTestOrmManager, atLeastOnce()).findById(publisher.getId(), Publisher.class);
        assertEquals(result, publisher);
    }

    @Test
    void findAllTest_ShouldReturnListOfPublishers() throws SQLException {
        //Given
        given(underTestOrmManager.findAll(Publisher.class)).willReturn(new ArrayList<>());

        //When
        var expected = underTestOrmManager.findAll(Publisher.class);

        //Then
        verify(underTestOrmManager, atLeastOnce()).findAll(Publisher.class);
    }

    @Test
    void deletePublisherTest_ShouldReturnTrue() {
        //Given
        Publisher publisher = new Publisher(1L, "test", List.of(new Book(), new Book()));

        //When
        when(underTestOrmManager.delete(publisher)).thenReturn(true);
        underTestOrmManager.delete(publisher);

        //Then
        verify(underTestOrmManager, atLeastOnce()).delete(publisher);
    }

    @Test
    @DisplayName("Update method for Publisher class")
    void mergePublisherTest_ShouldMergeAndReturnTrue() {
        //Given
        Publisher mergedPublisher = new Publisher(1L, "test", List.of(new Book(), new Book()));

        //When
        underTestOrmManager.merge(mergedPublisher);

        //Then
        verify(underTestOrmManager, atLeastOnce()).merge(mergedPublisher);
    }
}
