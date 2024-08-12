package com.depository_manage.service.clck.impl;

import com.depository_manage.entity.clck.Tax;
import com.depository_manage.mapper.clck.TaxMapper;
import com.depository_manage.service.clck.AreaService;
import com.depository_manage.service.clck.TaxService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
public class TaxServiceImpl implements TaxService {
    private final TaxMapper taxMapper;

    public TaxServiceImpl(TaxMapper taxMapper) {
        this.taxMapper = taxMapper;
    }

    @Override
    public List<Tax> findAll(Map<String, Object> params) {
        return taxMapper.findAll(params);
    }

    @Override
    public Tax findById(Integer id) {
        return taxMapper.findById(id);
    }

    @Override
    public void insert(Tax tax) {
        taxMapper.insert(tax);
    }

    @Override
    public void update(Tax tax) {
        taxMapper.update(tax);
    }

    @Override
    public void deleteById(Integer id) {taxMapper.deleteById(id);}
}
