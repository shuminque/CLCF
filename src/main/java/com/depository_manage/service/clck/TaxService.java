package com.depository_manage.service.clck;

import com.depository_manage.entity.clck.Tax;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TaxService {
    List<Tax> findAll(Map<String, Object> params);

    Tax findById(Integer id);

    void insert(Tax tax);

    void update(Tax tax);

    void deleteById(Integer id);
}
