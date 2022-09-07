package com.ormanager.client.entity;

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
@Table(name = "publishers")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Publisher {
    @Id
    private Long id;

    @NonNull
    private String name;

    @OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();

    public Publisher(Long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Publisher{" +
                "id=" + id +
                ", name='" + name + '\'' +
                getBooksToString()+
                '}';
    }

    private String getBooksToString(){
        if(books.isEmpty()){
            return "";
        }
        else {
            return books.toString();
        }
    }
}
