package com.prossafy101.ohnaco.entity.devtalk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prossafy101.ohnaco.entity.user.User;
import lombok.*;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int questionid;
    private String questiontitle;
    private String questioncontent;
    private LocalDateTime questiondate;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int views;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int likes;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    @ManyToMany
    @JoinTable(name = "tag_relation",
            joinColumns = @JoinColumn(name = "questionid"),
            inverseJoinColumns = @JoinColumn(name = "tagid"))
    private List<Tag> tag = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Answer> answeres;
}
