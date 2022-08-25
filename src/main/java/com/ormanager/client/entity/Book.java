package com.ormanager.client.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Table("books")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Book {
    @Id
    private Long id;

    @Column
    @NonNull
    private String title;

    @Column("published_at")
    @NonNull
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisher_id")
    private Publisher publisher = null;
}
