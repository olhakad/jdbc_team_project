package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "book")
@Data
@NoArgsConstructor
public class Book {
    @Id
    private Long id;

    @Column
    @NonNull
    private String title;

    @Column(name = "published_at")
    @NonNull
    private LocalDate publishedAt;

    /*public Book(Long id, @NonNull String title, @NonNull LocalDate publishedAt) {
        this.id = id;
        this.title = title;
        this.publishedAt = publishedAt;
    }*/

    public Book(@NonNull String title, @NonNull LocalDate publishedAt) {
        this.title = title;
        this.publishedAt = publishedAt;
    }

/*@ManyToOne(columnName = "publisher_id")
    private Publisher publisher = null;*/
}
