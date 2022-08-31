package com.ormanager.client.entity;

import com.ormanager.orm.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publishers")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Publisher implements Serializable {
    @Id
    private Long id;

    @Column
    @NonNull
    private String name;

    @OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();
}
