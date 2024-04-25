package com.depository_manage.service;

import com.depository_manage.entity.ShipmentDetails;
import java.util.List;

public interface ShipmentDetailsService {

    ShipmentDetails getShipmentDetailById(int id);
    List<ShipmentDetails> getAllShipmentDetails();
    boolean saveShipmentDetail(ShipmentDetails shipmentDetails);
    List<ShipmentDetails> saveAll(List<ShipmentDetails> shipmentDetailsList); // Change return type to List
    boolean updateShipmentDetail(ShipmentDetails shipmentDetails);
    boolean deleteShipmentDetailById(int id);
}
