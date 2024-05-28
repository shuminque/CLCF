package com.depository_manage.service.clck;

import com.depository_manage.entity.clck.DailyCounter;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DailyCounterService {
    List<DailyCounter> findAll(Map<String, Object> params);

    DailyCounter findById(Integer id);

    void insert(DailyCounter dailyCounter);

    void update(DailyCounter dailyCounter);

    void deleteById(Integer id);
    Integer findAndUpdateCounterByDate(LocalDate date);

}
