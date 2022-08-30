package com.ormanager.client.entity;

import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.annotation.Column;
import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@ToString
public class Book {
    @Id
    private Long id;
    private String title;
    @Column(name = "published_at")
    private LocalDate publishedAt;

    public Book() {
    }

    public Book(String title, LocalDate publishedAt) {
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }

//     2nd stage:
//     @ManyToOne(columnName = "publisher_id")
    Publisher publisher = null;
}