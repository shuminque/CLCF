package com.depository_manage.service.clck;

import com.depository_manage.entity.clck.Area;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AreaService {
    List<Area> findAll(Map<String, Object> params);

    Area findById(Integer id);

    void insert(Area area);

    void update(Area area);

    void deleteById(Integer id);
}
