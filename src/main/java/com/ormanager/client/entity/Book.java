package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@ToString
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Book {
    @Id
    private Long id;

    @Column
    @NonNull
    private String title;

    @Column(name = "published_at")
    @NonNull
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisher_id")
    Publisher publisher = null;

}