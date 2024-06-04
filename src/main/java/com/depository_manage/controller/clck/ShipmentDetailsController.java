package com.depository_manage.controller.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ProductId;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.BearingRecordService;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shipment")
public class ShipmentDetailsController {

    @Autowired
    private ShipmentDetailsService shipmentDetailsService;

    @Autowired
    private BearingRecordService bearingRecordService;

    @GetMapping
    public ResponseEntity<?> getAllRecords(  @RequestParam Map<String, Object> params,
                                             @RequestParam(defaultValue = "1") int page, // 页码通常是从1开始
                                             @RequestParam(defaultValue = "20") int size) {
        String dateRange = (String) params.get("time");
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] dates = dateRange.split(" - ");
            params.put("startDate", dates[0] + " 00:00:00");
            params.put("endDate", dates[1] + " 23:59:59");
        }
        int begin = (page - 1) * size;
        params.put("begin", begin);
        params.put("size", size);
        List<ShipmentDetails> records = shipmentDetailsService.getAllShipmentDetails(params);
        int count = shipmentDetailsService.countShipmentDetails(params);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "");
        response.put("count", count);
        response.put("data", records);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/record/{id}")
    public ResponseEntity<ShipmentDetails> getAllRecordsById(@PathVariable int id) {
        ShipmentDetails records = shipmentDetailsService.getShipmentDetailById(id);
        return ResponseEntity.ok(records);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBearingRecord(@PathVariable int id, @RequestBody ShipmentDetails record) {
        record.setId(id);
        shipmentDetailsService.updateShipmentDetail(record);
        return ResponseEntity.ok(record);
    }
    @PostMapping("/updateOperationType")
    public ResponseEntity<Void> updateOperationType(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            String operationType = request.get("operationType");
            String placementArea = request.get("placementArea");
            shipmentDetailsService.updateOperationTypeByUniqueIdentifier(uniqueIdentifier, operationType, placementArea);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if duplicate entry is attempted
        }
    }
    @PostMapping("/stockOut")
    public ResponseEntity<Void> stockOut(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            shipmentDetailsService.stockOut(uniqueIdentifier);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if stock out is not allowed
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody Map<String, String> request) {
        String uniqueIdentifier = request.get("uniqueIdentifier");
        String placementArea = request.get("placementArea");
        try {
            shipmentDetailsService.transfer(uniqueIdentifier, placementArea);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());  // Return 400 Bad Request with error message
        }
    }
    @PostMapping("/return")
    public ResponseEntity<Void> returnToStock(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            double weight = Double.parseDouble(request.get("weight"));
            shipmentDetailsService.returnToStock(uniqueIdentifier, weight);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if return operation is not allowed
        }
    }
    @GetMapping("/getStockStatusBeforeCutoffDate")
    public ResponseEntity<?> getStockStatusBeforeCutoffDate(@RequestParam Map<String, Object> params) {
        List<ShipmentDetails> records = shipmentDetailsService.getStockStatusBeforeCutoffDate(params);
        return ResponseEntity.ok(records);
    }
    @GetMapping("/viewTransfer")
    public ResponseEntity<?> viewTransfer(
            @RequestParam(required = false) String date) {
        Map<String, Object> params = new HashMap<>();

        // 检查日期参数是否存在且格式正确
        if (date != null && date.contains(" - ")) {
            // 分割字符串获取起始和结束日期
            String[] dates = date.split(" - ");
            if (dates.length == 2) {
                String startDate = dates[0] + " 00:00:00";
                String endDate   = dates[1] + " 23:59:59";
                params.put("startDate", startDate);
                params.put("endDate", endDate);
            }
        }
        // 调用服务层获取数据
        List<Map<String, Object>> transferRecords = shipmentDetailsService.viewTransfer(params);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "");
        response.put("count", transferRecords.size());
        response.put("data", transferRecords);
        return ResponseEntity.ok(response);
    }
        @GetMapping("/viewStockTake")
        public ResponseEntity<?> viewStockTake(  @RequestParam Map<String, Object> params) {
            String dateRange = (String) params.get("time");
            if (dateRange != null && dateRange.contains(" - ")) {
                String[] dates = dateRange.split(" - ");
                params.put("startDate", dates[0] + " 00:00:00");
                params.put("endDate", dates[1] + " 23:59:59");
            }
            List<Map<String, String>> result = shipmentDetailsService.getIntoSDs(params);
            for (Map<String, String> row : result) {
                String steel_mill= row.get("steel_mill");
                String steel_grade = row.get("steel_grade");
                String dimensions= row.get("dimensions");
                String trade_mode = row.get("trade_mode");
                String customer = row.get("customer");
                String startDate = params.get("startDate") != null ? params.get("startDate").toString() : null;
                String endDate = params.get("endDate") != null ? params.get("endDate").toString() : null;
                Map<String, Object> params2 = new HashMap<>();
                params2.put("steelMill", steel_mill);
                params2.put("steelGrade", steel_grade);
                params2.put("dimensions", dimensions);
                params2.put("tradeMode", trade_mode);
                params2.put("customer", customer);
                params2.put("startDate", startDate);
                params2.put("endDate", endDate);
                // 使用新的params2执行查询获取在库数量
                List<BearingRecord> records = bearingRecordService.getOutPDs(params2);
                if (!records.isEmpty()) {
                    BearingRecord firstRecord = records.get(0);
                    row.put("totalQuantityLA", String.valueOf(firstRecord.getTotalQuantityLA()));
                    row.put("totalWeighLA", String.valueOf(firstRecord.getTotalWeighLA()));
                    row.put("totalCountLA", String.valueOf(firstRecord.getTotalCountLA()));
                    row.put("totalQuantityLB", String.valueOf(firstRecord.getTotalQuantityLB()));
                    row.put("totalWeighLB", String.valueOf(firstRecord.getTotalWeighLB()));
                    row.put("totalCountLB", String.valueOf(firstRecord.getTotalCountLB()));
                } else {
                    row.put("totalQuantityLA", "0");
                    row.put("totalWeighLA", "0");
                    row.put("totalCountLA", "0");
                    row.put("totalQuantityLB", "0");
                    row.put("totalWeighLB", "0");
                    row.put("totalCountLB", "0");
                }
            }
            return ResponseEntity.ok(result);
        }
    @PostMapping("/getWeight")
    public ResponseEntity<Map<String, Object>> getWeight(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            double weight = shipmentDetailsService.getWeightByUniqueIdentifier(uniqueIdentifier);
            Map<String, Object> response = new HashMap<>();
            response.put("uniqueIdentifier", uniqueIdentifier);
            response.put("weight", weight);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if the uniqueIdentifier is not found
        }
    }



}
