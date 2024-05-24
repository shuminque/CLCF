package com.depository_manage.service.clck;

import com.depository_manage.entity.ShipmentDetails;
import java.util.List;
import java.util.Map;

public interface ShipmentDetailsService {

    ShipmentDetails getShipmentDetailById(int id);
    int countShipmentDetails(Map<String, Object> params);
    List<ShipmentDetails> getAllShipmentDetails(Map<String, Object> params);
    boolean saveShipmentDetail(ShipmentDetails shipmentDetails);
    List<ShipmentDetails> saveAll(List<ShipmentDetails> shipmentDetailsList); // Change return type to List
    boolean updateShipmentDetail(ShipmentDetails shipmentDetails);
    boolean deleteShipmentDetailById(int id);
    void updateOperationTypeBySupplierBatchNumber(String supplierBatchNumber, String operationType, String placementArea) throws Exception;
    void stockOut(String supplierBatchNumber) throws Exception;
    void transfer(String supplierBatchNumber, String placementArea) throws Exception;  // 新增的方法
    List<ShipmentDetails> getStockStatusBeforeCutoffDate(Map<String, Object> params);

}
