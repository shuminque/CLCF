package com.depository_manage.service.clck;

import com.depository_manage.entity.ShipmentDetails;
import java.util.List;

public interface ShipmentDetailsService {

    ShipmentDetails getShipmentDetailById(int id);
    List<ShipmentDetails> getAllShipmentDetails();
    boolean saveShipmentDetail(ShipmentDetails shipmentDetails);
    List<ShipmentDetails> saveAll(List<ShipmentDetails> shipmentDetailsList); // Change return type to List
    boolean updateShipmentDetail(ShipmentDetails shipmentDetails);
    boolean deleteShipmentDetailById(int id);
    void updateOperationTypeBySupplierBatchNumber(String supplierBatchNumber, String operationType, String placementArea) throws Exception;
    void stockOut(String supplierBatchNumber) throws Exception;
    void transfer(String supplierBatchNumber, String placementArea) throws Exception;  // 新增的方法

}
