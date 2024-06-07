package com.depository_manage.service.clck.impl;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.mapper.clck.ShipmentDetailsMapper;
import com.depository_manage.service.clck.ShipmentDetailsService;
import com.depository_manage.utils.ObjectFormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
    public int countShipmentDetails(Map<String, Object> params) {
        return shipmentDetailsMapper.countShipmentDetails(params);
    }
    @Override
    public List<ShipmentDetails> getAllShipmentDetails(Map<String, Object> params) {
        Integer size = 8, page = 1;
        if (params.containsKey("size")) {
            size = ObjectFormatUtil.toInteger(params.get("size"));
            params.put("size", size);
        }
        if (params.containsKey("page")) {
            page = ObjectFormatUtil.toInteger(params.get("page"));
            params.put("begin", (page - 1) * size);
        }
        return shipmentDetailsMapper.selectAllShipmentDetails(params);    }

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
    public void updateShipmentDetail(ShipmentDetails shipmentDetails) {
        shipmentDetailsMapper.updateShipmentDetail(shipmentDetails);
    }
    @Override
    @Transactional
    public boolean deleteShipmentDetailById(int id) {
        return shipmentDetailsMapper.deleteShipmentDetail(id) > 0;
    }

    @Override
    public void updateOperationTypeByUniqueIdentifier(String uniqueIdentifier, String operationType, String placementArea) throws Exception {
        if ("入库".equals(operationType)) {
            if (!canStockIn(uniqueIdentifier)) {
                throw new Exception("该批次号已入库，不能重复入库");
            }
        }
        List<ShipmentDetails> stockInRecords = shipmentDetailsMapper.findStockInByUniqueIdentifier(uniqueIdentifier);
        if (!stockInRecords.isEmpty()) {
            ShipmentDetails stockInRecord = stockInRecords.get(0);
            // 创建出库记录
            ShipmentDetails stockOutRecord = new ShipmentDetails();
            stockOutRecord.setUniqueIdentifier(stockInRecord.getUniqueIdentifier());
            stockOutRecord.setInvoiceNumber(stockInRecord.getInvoiceNumber());
            stockOutRecord.setCustomer(stockInRecord.getCustomer());
            stockOutRecord.setTradeMode(stockInRecord.getTradeMode());
            stockOutRecord.setDeliveryPoint(stockInRecord.getDeliveryPoint());
            stockOutRecord.setArrivalPortDate(stockInRecord.getArrivalPortDate());
            stockOutRecord.setArrivalDate(stockInRecord.getArrivalDate());
            stockOutRecord.setSteelGrade(stockInRecord.getSteelGrade());
            stockOutRecord.setDimensions(stockInRecord.getDimensions());
            stockOutRecord.setWeight(stockInRecord.getWeight());
            stockOutRecord.setSteelMill(stockInRecord.getSteelMill());
            stockOutRecord.setFurnaceNumber(stockInRecord.getFurnaceNumber());
            stockOutRecord.setInvoiceApplication(stockInRecord.getInvoiceApplication());
            stockOutRecord.setOperationType("入库");
            stockOutRecord.setSupplierBatchNumber(stockInRecord.getSupplierBatchNumber());
            stockOutRecord.setBundleCount(stockInRecord.getBundleCount());
            stockOutRecord.setPlacementArea(placementArea);
            stockOutRecord.setPurchaser(stockInRecord.getPurchaser());
            stockOutRecord.setTime(new Date()); // 设置当前时间
            shipmentDetailsMapper.insertShipmentDetail(stockOutRecord);
        }else {
            ShipmentDetails updateRecord = new ShipmentDetails();
            updateRecord.setUniqueIdentifier(uniqueIdentifier);
            updateRecord.setOperationType(operationType);
            updateRecord.setPlacementArea(placementArea);
            updateRecord.setTime(new Date()); // 设置当前时间
            shipmentDetailsMapper.updateOperationTypeByUniqueIdentifier(updateRecord);
        }
    }

    private boolean canStockIn(String uniqueIdentifier) {
        int netStockInCount = shipmentDetailsMapper.getNetStockInCountByUniqueIdentifier(uniqueIdentifier);
        return netStockInCount <= 0;
    }

    @Override
    public void stockOut(String uniqueIdentifier) throws Exception {
        int netStockInCount = shipmentDetailsMapper.getNetStockInCountByUniqueIdentifier(uniqueIdentifier);
        if (netStockInCount <= 0) {
            throw new Exception("当前批次号没有可出库的库存");
        }
        // 根据 uniqueIdentifier 和 operationType 为 '入库' 查找记录
        List<ShipmentDetails> stockInRecords = shipmentDetailsMapper.findStockInByUniqueIdentifier(uniqueIdentifier);
        if (stockInRecords != null) {
            ShipmentDetails stockInRecord = stockInRecords.get(0);
            // 创建出库记录
            ShipmentDetails stockOutRecord = new ShipmentDetails();
            stockOutRecord.setUniqueIdentifier(stockInRecord.getUniqueIdentifier());
            stockOutRecord.setInvoiceNumber(stockInRecord.getInvoiceNumber());
            stockOutRecord.setCustomer(stockInRecord.getCustomer());
            stockOutRecord.setTradeMode(stockInRecord.getTradeMode());
            stockOutRecord.setDeliveryPoint(stockInRecord.getDeliveryPoint());
            stockOutRecord.setArrivalPortDate(stockInRecord.getArrivalPortDate());
            stockOutRecord.setArrivalDate(stockInRecord.getArrivalDate());
            stockOutRecord.setSteelGrade(stockInRecord.getSteelGrade());
            stockOutRecord.setDimensions(stockInRecord.getDimensions());
            stockOutRecord.setWeight(stockInRecord.getWeight());
            stockOutRecord.setSteelMill(stockInRecord.getSteelMill());
            stockOutRecord.setFurnaceNumber(stockInRecord.getFurnaceNumber());
            stockOutRecord.setInvoiceApplication(stockInRecord.getInvoiceApplication());
            stockOutRecord.setOperationType("出库");
            stockOutRecord.setSupplierBatchNumber(stockInRecord.getSupplierBatchNumber());
            stockOutRecord.setBundleCount(stockInRecord.getBundleCount());
            stockOutRecord.setPlacementArea(stockInRecord.getPlacementArea());
            stockOutRecord.setPurchaser(stockInRecord.getPurchaser());
            stockOutRecord.setTime(new Date()); // 设置当前时间
            // 插入出库记录
            shipmentDetailsMapper.insertShipmentDetail(stockOutRecord);
        }
    }
    @Override
    public void transfer(String uniqueIdentifier, String placementArea) throws Exception {
        // 检查净入库记录数是否大于0
        int netStockInCount = shipmentDetailsMapper.getNetStockInCountByUniqueIdentifier(uniqueIdentifier);
        if (netStockInCount <= 0) {
            throw new Exception("该产品无库存，操作无效");
        }
        List<ShipmentDetails> records = shipmentDetailsMapper.findStockInOrTransferByUniqueIdentifier(uniqueIdentifier);
        if (!records.isEmpty()) {
            ShipmentDetails record = records.get(0);
            // 检查选择的库位是否与原库位相同
            if (placementArea.equals(record.getPlacementArea())) {
                throw new Exception("选择的库位与原库位相同，请选择不同的库位");
            }
            ShipmentDetails transferRecord = new ShipmentDetails();
            transferRecord.setUniqueIdentifier(record.getUniqueIdentifier());
            transferRecord.setInvoiceNumber(record.getInvoiceNumber());
            transferRecord.setCustomer(record.getCustomer());
            transferRecord.setTradeMode(record.getTradeMode());
            transferRecord.setDeliveryPoint(record.getDeliveryPoint());
            transferRecord.setArrivalPortDate(record.getArrivalPortDate());
            transferRecord.setArrivalDate(record.getArrivalDate());
            transferRecord.setSteelGrade(record.getSteelGrade());
            transferRecord.setDimensions(record.getDimensions());
            transferRecord.setWeight(record.getWeight());
            transferRecord.setSteelMill(record.getSteelMill());
            transferRecord.setFurnaceNumber(record.getFurnaceNumber());
            transferRecord.setInvoiceApplication(record.getInvoiceApplication());
            transferRecord.setOperationType("转库");
            transferRecord.setSupplierBatchNumber(record.getSupplierBatchNumber());
            transferRecord.setBundleCount(record.getBundleCount());
            transferRecord.setPlacementArea(placementArea);
            transferRecord.setPurchaser(record.getPurchaser());
            transferRecord.setTime(new Date()); // 设置当前时间
            shipmentDetailsMapper.insertShipmentDetail(transferRecord);
        }
    }
    public void returnToStock(String uniqueIdentifier, double weight)throws Exception {
        if (!canStockIn(uniqueIdentifier)) {
            throw new Exception("该批次号已在库，不能重复入库");
        }
        List<ShipmentDetails> stockInRecords = shipmentDetailsMapper.findStockInByUniqueIdentifier(uniqueIdentifier);
        if (!stockInRecords.isEmpty()) {
            ShipmentDetails stockInRecord = stockInRecords.get(0);
            // 创建出库记录
            ShipmentDetails stockOutRecord = new ShipmentDetails();
            stockOutRecord.setUniqueIdentifier(stockInRecord.getUniqueIdentifier());
            stockOutRecord.setInvoiceNumber(stockInRecord.getInvoiceNumber());
            stockOutRecord.setCustomer(stockInRecord.getCustomer());
            stockOutRecord.setTradeMode(stockInRecord.getTradeMode());
            stockOutRecord.setDeliveryPoint(stockInRecord.getDeliveryPoint());
            stockOutRecord.setArrivalPortDate(stockInRecord.getArrivalPortDate());
            stockOutRecord.setArrivalDate(stockInRecord.getArrivalDate());
            stockOutRecord.setSteelGrade(stockInRecord.getSteelGrade());
            stockOutRecord.setDimensions(stockInRecord.getDimensions());
            stockOutRecord.setWeight(weight);
            stockOutRecord.setSteelMill(stockInRecord.getSteelMill());
            stockOutRecord.setFurnaceNumber(stockInRecord.getFurnaceNumber());
            stockOutRecord.setInvoiceApplication(stockInRecord.getInvoiceApplication());
            stockOutRecord.setOperationType("返库");
            stockOutRecord.setSupplierBatchNumber(stockInRecord.getSupplierBatchNumber());
            stockOutRecord.setBundleCount(stockInRecord.getBundleCount());
            stockOutRecord.setPlacementArea(stockInRecord.getPlacementArea());
            stockOutRecord.setPurchaser(stockInRecord.getPurchaser());
            stockOutRecord.setTime(new Date()); // 设置当前时间
            shipmentDetailsMapper.insertShipmentDetail(stockOutRecord);
        }
    }

    @Override
    public List<ShipmentDetails> getStockStatusBeforeCutoffDate(Map<String, Object> params) {
        Integer size = 8, page = 1;
        if (params.containsKey("size")) {
            size = ObjectFormatUtil.toInteger(params.get("size"));
            params.put("size", size);
        }
        if (params.containsKey("page")) {
            page = ObjectFormatUtil.toInteger(params.get("page"));
            params.put("begin", (page - 1) * size);
        }
        return shipmentDetailsMapper.getStockStatusBeforeCutoffDate(params);
    }
    @Override
    public List<Map<String, Object>> viewTransfer(Map<String, Object> params) {
        return shipmentDetailsMapper.viewTransfer(params);
    }

    @Override
    public List<Map<String, String>> getIntoSDs(Map<String, Object> params) {
        return shipmentDetailsMapper.getIntoSDs(params);
    }
    public double getWeightByUniqueIdentifier(String uniqueIdentifier) {
        return shipmentDetailsMapper.getWeightByUniqueIdentifier(uniqueIdentifier);
    }


}
