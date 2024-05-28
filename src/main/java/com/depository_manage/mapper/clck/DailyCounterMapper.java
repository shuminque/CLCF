package com.depository_manage.mapper.clck;

import com.depository_manage.entity.clck.DailyCounter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface DailyCounterMapper {
    List<DailyCounter> findAll(Map<String, Object> params);

    DailyCounter findById(@Param("id") Integer id);

    int insert(DailyCounter dailyCounter);

    int update(DailyCounter dailyCounter);

    int deleteById(@Param("id") Integer id);

    Integer findCounterByDate(LocalDate date);

    void updateCounterByDate(@Param("date") LocalDate date, @Param("counter") Integer counter);
}
