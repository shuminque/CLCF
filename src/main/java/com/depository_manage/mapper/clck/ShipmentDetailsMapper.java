package com.depository_manage.mapper.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ShipmentDetailsMapper {

    // Existing methods
    int insertShipmentDetail(ShipmentDetails shipmentDetails);
    ShipmentDetails selectShipmentDetailById(@Param("id") int id);
    // 获取所有记录
    int countShipmentDetails(Map<String, Object> params);
    List<ShipmentDetails> selectAllShipmentDetails(Map<String,Object> params);
    int updateShipmentDetail(ShipmentDetails shipmentDetails);
    int deleteShipmentDetail(@Param("id") int id);

    // New method for batch insertion
    int insertShipmentDetails(List<ShipmentDetails> shipmentDetailsList);
    void updateOperationTypeBySupplierBatchNumber(@Param("supplierBatchNumber") String supplierBatchNumber, @Param("operationType") String operationType, @Param("placementArea") String placementArea);
    List<ShipmentDetails> findStockInBySupplierBatchNumber(@Param("supplierBatchNumber") String supplierBatchNumber);
    List<ShipmentDetails> findStockInOrTransferBySupplierBatchNumber(@Param("supplierBatchNumber") String supplierBatchNumber);
    int getNetStockInCountBySupplierBatchNumber(@Param("supplierBatchNumber") String supplierBatchNumber);

    List<ShipmentDetails> getStockStatusBeforeCutoffDate(Map<String, Object> params);
    List<Map<String, Object>> viewTransfer(Map<String, Object> params);

}
