package com.depository_manage.mapper.clck;

import com.depository_manage.entity.clck.Purchaser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PurchaserMapper {
    List<Purchaser> findAll();

    Purchaser findById(@Param("id") Integer id);

    int insert(Purchaser purchaser);

    int update(Purchaser purchaser);

    int deleteById(@Param("id") Integer id);
}
