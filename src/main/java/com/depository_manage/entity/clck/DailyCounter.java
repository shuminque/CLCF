package com.depository_manage.entity.clck;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyCounter {
    private Long id;
    private LocalDate date;
    private int counter;
}
