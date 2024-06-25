package com.depository_manage.mapper.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ShipmentDetailsMapper {

    int insertShipmentDetail(ShipmentDetails shipmentDetails);

    ShipmentDetails selectShipmentDetailById(@Param("id") int id);

    int countShipmentDetails(Map<String, Object> params);

    List<ShipmentDetails> selectAllShipmentDetails(Map<String,Object> params);

    void updateShipmentDetail(ShipmentDetails shipmentDetails);

    int deleteShipmentDetail(@Param("id") int id);
    int batchDeleteShipmentDetails(@Param("ids") List<Integer> ids);

    int insertShipmentDetails(List<ShipmentDetails> shipmentDetailsList);

    void updateOperationTypeByUniqueIdentifier(ShipmentDetails shipmentDetails);

    List<ShipmentDetails> findStockInByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);

    List<ShipmentDetails> findStockInOrTransferByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);

    int getNetStockInCountByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);

    List<ShipmentDetails> getStockStatusBeforeCutoffDate(Map<String, Object> params);

    List<Map<String, Object>> viewTransfer(Map<String, Object> params);

    List<ShipmentDetails> getIntoSDs(Map<String, Object> params);


    double getWeightByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);

    List<Map<String, Object>> getclstatus(@Param("date") Date cutoffDate);
    List<Map<String, Object>> getClstatusByDimensions(@Param("date") Date cutoffDate);

    List<Map<String, Object>> getMonthlyInOutWeightByYear(int year);

    List<Map<String, Object>> getMonthlyCumulativeInventoryStatusByYear(int year);
}
