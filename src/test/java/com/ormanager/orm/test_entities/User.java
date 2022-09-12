package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.Entity;
import com.ormanager.orm.annotation.Id;
import com.ormanager.orm.annotation.OneToMany;
import com.ormanager.orm.annotation.Table;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class User {
    @Id
    Long id;
    @NonNull
    String name;
    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", comments=" + comments +
                '}';
    }

    private String getCommentDetails(){
        if(name==""){
            return "";
        }
        else {
            return comments.toString();
        }
    }
}
