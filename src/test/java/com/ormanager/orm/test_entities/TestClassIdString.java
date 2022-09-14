package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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