package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.OneToMany;
import com.ormanager.orm.annotation.Table;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "publishersWithUuidId")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class TestClassPublisherWIthUuidId {
    @Id
    private UUID id;

    @NonNull
    private String name;

    @OneToMany(mappedBy = "publisherWithUuidId")
    private List<TestClassBookWithUuidId> booksWithUuidId = new ArrayList<>();

    public TestClassPublisherWIthUuidId(UUID id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "PublisherWithUuidId{" +
                "id=" + id +
                ", name='" + name + '\'' +
                getBooksToString() +
                '}';
    }

    private String getBooksToString() {
        if (booksWithUuidId.isEmpty()) {
            return "";
        } else {
            return booksWithUuidId.toString();
        }
    }
}
