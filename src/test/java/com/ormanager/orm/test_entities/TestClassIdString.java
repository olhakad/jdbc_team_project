package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.*;
import lombok.*;

@Entity
@Table(name = "test_id_string")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class TestClassIdString {

    @Id
    private String id;

    @NonNull
    private String title;
}