package com.depository_manage.service.clck.impl;

import com.depository_manage.entity.clck.DailyCounter;
import com.depository_manage.mapper.clck.DailyCounterMapper;
import com.depository_manage.service.clck.DailyCounterService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DailyCounterServiceImpl implements DailyCounterService {

    private final DailyCounterMapper dailyCounterMapper;

    public DailyCounterServiceImpl(DailyCounterMapper dailyCounterMapper) {
        this.dailyCounterMapper = dailyCounterMapper;
    }

    @Override
    public List<DailyCounter> findAll(Map<String, Object> params) {
        return dailyCounterMapper.findAll(params);
    }

    @Override
    public DailyCounter findById(Integer id) {
        return dailyCounterMapper.findById(id);
    }

    @Override
    public void insert(DailyCounter dailyCounter) {
        dailyCounterMapper.insert(dailyCounter);
    }

    @Override
    public void update(DailyCounter dailyCounter) {
        dailyCounterMapper.update(dailyCounter);
    }

    @Override
    public void deleteById(Integer id) {dailyCounterMapper.deleteById(id);}

    @Override
    public Integer findAndUpdateCounterByDate(LocalDate date) {
        Integer counter = dailyCounterMapper.findCounterByDate(date);
        if (counter == null) {
            counter = 1;
            DailyCounter dailyCounter = new DailyCounter();
            dailyCounter.setDate(date);
            dailyCounter.setCounter(counter);
            dailyCounterMapper.insert(dailyCounter); // 插入新日期的记录
        } else {
            counter++; // 序号加一
            dailyCounterMapper.updateCounterByDate(date, counter); // 更新序号
        }
        return counter;
    }
}
