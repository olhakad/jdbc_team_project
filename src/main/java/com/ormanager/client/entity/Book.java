package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Book {
    @Id
    private Long id;

    @NonNull
    private String title;

    @Column(name = "published_at")
    @NonNull
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisher_id")
    Publisher publisher = null;

    public Book(Long id, @NonNull String title, @NonNull LocalDate publishedAt) {
        this.id = id;
        this.title = title;
        this.publishedAt = publishedAt;
    }
}
