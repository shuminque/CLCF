package com.depository_manage.mapper.clck;

import com.depository_manage.entity.Rate;
import com.depository_manage.entity.clck.Tax;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Mapper
public interface TaxMapper {
    List<Tax> findAll(Map<String, Object> params);

    Tax findById(@Param("id") Integer id);

    int insert(Tax tax);

    int update(Tax tax);

    int deleteById(@Param("id") Integer id);
}
