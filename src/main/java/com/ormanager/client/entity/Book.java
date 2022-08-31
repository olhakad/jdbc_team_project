package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

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
    private String title;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisher_id")
    Publisher publisher = null;
}