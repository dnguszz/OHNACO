package com.prossafy101.ohnaco.entity.todo;

import lombok.Data;

import java.sql.Date;
import java.sql.Time;

@Data
public class TodoDto {

    private String title;
    private String categoryid;
    private String goaltime;
    private String date;
    private String userid;
    private String todoid;
    private Time completetime;
    private Boolean ongoing;
}
