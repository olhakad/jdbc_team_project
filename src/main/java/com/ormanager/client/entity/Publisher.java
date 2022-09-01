package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publishers")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Publisher implements Serializable {
    @Id
    private Long id;

    @Column
    @NonNull
    private String name;

    @OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();

    public Publisher(Long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    public Publisher(@NonNull String name, List<Book> books) {
        this.name = name;
        this.books = books;
    }
}
