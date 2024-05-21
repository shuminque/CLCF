package com.depository_manage.service.clck;

import com.depository_manage.entity.clck.Purchaser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PurchaserService {
    List<Purchaser> findAll();

    Purchaser findById(Integer id);

    void insert(Purchaser purchaser);

    void update(Purchaser purchaser);

    void deleteById(Integer id);

}
