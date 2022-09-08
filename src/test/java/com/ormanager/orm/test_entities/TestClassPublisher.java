package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.OneToMany;
import com.ormanager.orm.annotation.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_publishers")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class TestClassPublisher implements Serializable {
    @Id
    private Long id;

    @NonNull
    private String name;

    @OneToMany(mappedBy = "publisher")
    private List<TestClassBook> books = new ArrayList<>();
}