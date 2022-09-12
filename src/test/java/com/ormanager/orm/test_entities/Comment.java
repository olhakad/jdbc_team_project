package com.ormanager.orm.test_entities;

import com.ormanager.orm.annotation.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Comment {
    @Id
    private Long id;

    @NonNull
    private String content;

    @Column(name = "created_at")
    @NonNull
    private LocalDate createdAt;

    @ManyToOne(columnName = "user_id")
    User user = null;

    public Comment(Long id, @NonNull String content, @NonNull LocalDate createdAt) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                getCommentDetails()
                +
                '}';
    }
    private String getCommentDetails(){
        if(user!=null){
            return ", user=[" + user.getName() + ",id=" + user.getId() +"]";
        }
        else {
            return "";
        }
    }
}
