package com.depository_manage.entity.clck;

import lombok.Data;

import java.util.Date;

@Data
public class Tax {
    private Integer id;
    private Date date;
    private Double rate;
    private String introduce;
}
