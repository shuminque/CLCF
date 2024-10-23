package com.depository_manage.mapper.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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
    ShipmentDetails getLastCustomer(@Param("steelMill") String steelMill,
                           @Param("steelGrade") String steelGrade,
                           @Param("dimensions") String dimensions,
                           @Param("tradeMode") String tradeMode);
    List<Map<String, Object>> getclstatus(@Param("date") Date cutoffDate);
    List<Map<String, Object>> getClstatusByDimensions(@Param("date") Date cutoffDate);
    List<Map<String, Object>> getPipeByDimensions(@Param("date") Date cutoffDate);
    List<Map<String, Object>> getForgByDimensions(@Param("date") Date cutoffDate);
    List<Map<String, Object>> getMonthlyInOutWeightByYear(int year);

    List<Map<String, Object>> getMonthlyCumulativeInventoryStatusByYear(int year);

    List<Map<String, Object>> queryShipmentDetails(Map<String, Object> params);
    void updateInvoiceApplication(
            @Param("arrival_date") String arrival_date,
            @Param("steel_mill") String steel_mill,
            @Param("steel_grade") String steel_grade,
            @Param("dimensions") String dimensions,
            @Param("trade_mode") String trade_mode,
            @Param("invoice_application") String invoice_application,
            @Param("original_unit_price") BigDecimal original_unit_price,
            @Param("new_unit_price") BigDecimal new_unit_price
    );

    List<Map<String, Object>> selectInvoiceApplication(@Param("params") Map<String, Object> params);

    void updateUnitPrice(@Param("uniqueIdentifier") String uniqueIdentifier, @Param("unitPrice") double unitPrice);
    List<Map<String, Object>> getShipmentDetails(Map<String, Object> params);

}
