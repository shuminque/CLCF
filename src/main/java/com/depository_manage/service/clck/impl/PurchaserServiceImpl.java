package com.depository_manage.service.clck.impl;

import com.depository_manage.entity.clck.Purchaser;
import com.depository_manage.mapper.clck.PurchaserMapper;
import com.depository_manage.service.clck.PurchaserService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PurchaserServiceImpl implements PurchaserService {

    private final PurchaserMapper purchaserMapper;

    public PurchaserServiceImpl(PurchaserMapper purchaserMapper) {
        this.purchaserMapper = purchaserMapper;
    }

    @Override
    public List<Purchaser> findAll() {
        return purchaserMapper.findAll();
    }

    @Override
    public Purchaser findById(Integer id) {
        return purchaserMapper.findById(id);
    }

    @Override
    public void insert(Purchaser purchaser) {
        purchaserMapper.insert(purchaser);
    }

    @Override
    public void update(Purchaser purchaser) {
        purchaserMapper.update(purchaser);
    }

    @Override
    public void deleteById(Integer id) {purchaserMapper.deleteById(id);}
}
