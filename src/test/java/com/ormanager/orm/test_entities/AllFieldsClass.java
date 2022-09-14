package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "all_fields")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class AllFieldsClass {
    @Id
    private Long id;
    @NonNull
    private long longTest;
    @NonNull
    private int intTest;
    @NonNull
    private Integer wrapperIntegerTest;
    @NonNull
    private double doubleTest;
    @NonNull
    private Double wrapperDoubleTest;
    @NonNull
    private boolean booleanTest;
    @NonNull
    private Boolean wrapperBooleanTest;
    @NonNull
    private String stringTest;
    @NonNull
    private LocalDate localDateTest;
    @NonNull
    private LocalTime localTimeTest;
    @NonNull
    private LocalDateTime localDateTimeTest;
    public boolean getBooleanTest(){
        return booleanTest;
    }
}
