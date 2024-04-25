package com.depository_manage.service.impl;

import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.mapper.clck.ShipmentDetailsMapper;
import com.depository_manage.service.ShipmentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShipmentDetailsServiceImpl implements ShipmentDetailsService {

    private final ShipmentDetailsMapper shipmentDetailsMapper;

    @Autowired
    public ShipmentDetailsServiceImpl(ShipmentDetailsMapper shipmentDetailsMapper) {
        this.shipmentDetailsMapper = shipmentDetailsMapper;
    }

    @Override
    public ShipmentDetails getShipmentDetailById(int id) {
        return shipmentDetailsMapper.selectShipmentDetailById(id);
    }

    @Override
    public List<ShipmentDetails> getAllShipmentDetails() {
        return shipmentDetailsMapper.selectAllShipmentDetails();
    }

    @Override
    public boolean saveShipmentDetail(ShipmentDetails shipmentDetails) {
        return shipmentDetailsMapper.insertShipmentDetail(shipmentDetails) > 0;
    }
    @Override
    public List<ShipmentDetails> saveAll(List<ShipmentDetails> shipmentDetailsList) {
        if (shipmentDetailsList.isEmpty()) {
            return shipmentDetailsList; // Return the empty list if there's nothing to save
        }
        int count = shipmentDetailsMapper.insertShipmentDetails(shipmentDetailsList);
        if (count == shipmentDetailsList.size()) {
            return shipmentDetailsList;
        } else {
            throw new RuntimeException("Some entities could not be saved"); // Or handle the partial save appropriately
        }
    }
    @Override
    @Transactional
    public boolean updateShipmentDetail(ShipmentDetails shipmentDetails) {
        return shipmentDetailsMapper.updateShipmentDetail(shipmentDetails) > 0;
    }

    @Override
    @Transactional
    public boolean deleteShipmentDetailById(int id) {
        return shipmentDetailsMapper.deleteShipmentDetail(id) > 0;
    }
}
