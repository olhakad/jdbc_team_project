package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "test_books")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class TestClassBook {

    @Id
    private Long id;

    @NonNull
    private String title;

    @Column(name = "published_at")
    @NonNull
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisher_id")
    TestClassPublisher publisher = null;
}