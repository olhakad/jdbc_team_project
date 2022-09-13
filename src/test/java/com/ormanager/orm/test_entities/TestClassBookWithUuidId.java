package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booksWithUuidId")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class TestClassBookWithUuidId {
    @Id
    private UUID id;

    @NonNull
    private String title;

    @Column(name = "published_at")
    @NonNull
    private LocalDate publishedAt;

    @ManyToOne(columnName = "publisherWithUuidId_id")
    TestClassPublisherWIthUuidId publisherWithUuidId = null;

    public TestClassBookWithUuidId(UUID id, @NonNull String title, @NonNull LocalDate publishedAt) {
        this.id = id;
        this.title = title;
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return "BookWithUuidId{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", publishedAt=" + publishedAt +
                getPublisherDetails()
                +
                '}';
    }

    private String getPublisherDetails() {
        if (publisherWithUuidId != null) {
            return ", publisherWithUuidId=[" + publisherWithUuidId.getName() + ",id=" + publisherWithUuidId.getId() + "]";
        } else {
            return "";
        }
    }
}
