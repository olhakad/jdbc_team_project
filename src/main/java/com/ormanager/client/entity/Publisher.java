package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publishers")
@Data
@NoArgsConstructor
public class Publisher {

    @Id
    private Long id;

    public Publisher(Long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Column
    @NonNull
    private String name;



    /*@OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();*/
}
