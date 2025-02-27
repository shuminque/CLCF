package com.depository_manage.service.clck;

import com.depository_manage.entity.ShipmentDetails;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ShipmentDetailsService {

    ShipmentDetails getShipmentDetailById(int id);
    int countShipmentDetails(Map<String, Object> params);
    List<ShipmentDetails> getAllShipmentDetails(Map<String, Object> params);
    boolean saveShipmentDetail(ShipmentDetails shipmentDetails);
    List<ShipmentDetails> saveAll(List<ShipmentDetails> shipmentDetailsList); // Change return type to List
    void updateShipmentDetail(ShipmentDetails shipmentDetails);
    boolean deleteShipmentDetailById(int id);
    boolean batchDeleteShipmentDetails(List<Integer> ids);
    void updateOperationTypeByUniqueIdentifier(String uniqueIdentifier, String operationType, String placementArea) throws Exception;
    void stockOut(String uniqueIdentifier) throws Exception;
    void tuiHuo(String uniqueIdentifier) throws Exception;
    String transfer(String uniqueIdentifier, String placementArea) throws Exception;  // 新增的方法
    void returnToStock(String uniqueIdentifier, double weight, String placementArea)throws Exception;
    List<ShipmentDetails> getStockStatusBeforeCutoffDate(Map<String, Object> params);
    List<Map<String, Object>> viewTransfer(Map<String, Object> params);
    List<ShipmentDetails> getIntoSDs(Map<String, Object> params);

    double getWeightByUniqueIdentifier(String uniqueIdentifier);
    ShipmentDetails getLastCustomer(String steelMill, String steelGrade, String dimensions, String tradeMode);
    List<Map<String, Object>> getclstatus(Date cutoffDate);

    List<Map<String, Object>> getClstatusByDimensions(Date cutoffDate);
    List<Map<String, Object>> getPipeByDimensions(Date cutoffDate);
    List<Map<String, Object>> getForgByDimensions(Date cutoffDate);
    List<Map<String, Object>> getMonthlyInOutWeightByYear(int year);

    List<Map<String, Object>> getMonthlyCumulativeInventoryStatusByYear(int year);

    List<Map<String, Object>> queryShipmentDetails(Map<String, Object> params);

    void updateInvoiceApplication(String arrival_date, String steel_mill, String steel_grade, String dimensions, String trade_mode, String invoice_application, BigDecimal original_unit_price, BigDecimal new_unit_price);
    List<Map<String, Object>> selectInvoiceApplication(Map<String, Object> params);

    void updateUnitPrice(String uniqueIdentifier, double unitPrice);
    List<Map<String, Object>> fetchShipmentDetails(Map<String, Object> params);
    List<Map<String, Object>> getMonthlyInventory(String state, String year);

}
