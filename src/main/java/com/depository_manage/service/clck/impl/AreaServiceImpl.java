package com.depository_manage.service.clck.impl;

import com.depository_manage.entity.clck.Area;
import com.depository_manage.mapper.clck.AreaMapper;
import com.depository_manage.service.clck.AreaService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AreaServiceImpl implements AreaService {

    private final AreaMapper areaMapper;

    public AreaServiceImpl(AreaMapper areaMapper) {
        this.areaMapper = areaMapper;
    }

    @Override
    public List<Area> findAll(Map<String, Object> params) {
        return areaMapper.findAll(params);
    }

    @Override
    public Area findById(Integer id) {
        return areaMapper.findById(id);
    }

    @Override
    public void insert(Area area) {
        areaMapper.insert(area);
    }

    @Override
    public void update(Area area) {
        areaMapper.update(area);
    }

    @Override
    public void deleteById(Integer id) {areaMapper.deleteById(id);}
}
