package com.depository_manage.mapper.clck;

import com.depository_manage.entity.clck.Area;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AreaMapper {
    List<Area> findAll(Map<String, Object> params);

    Area findById(@Param("id") Integer id);

    int insert(Area area);

    int update(Area area);

    int deleteById(@Param("id") Integer id);
}
