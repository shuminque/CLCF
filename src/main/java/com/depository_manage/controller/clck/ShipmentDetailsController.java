package com.depository_manage.controller.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RestController
    @RequestMapping("/records")
    public class RecordsController {

        @Autowired
        private ShipmentDetailsService shipmentDetailsService;

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
        @GetMapping("/{id}")
        public ResponseEntity<ShipmentDetails> getAllRecords(@PathVariable int id) {
            ShipmentDetails records = shipmentDetailsService.getShipmentDetailById(id);
            return ResponseEntity.ok(records);
        }

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
        List<ShipmentDetails> records = shipmentDetailsService.getIntoSDs(params);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "");
        response.put("count", records.size());
        response.put("data", records);
        return ResponseEntity.ok(response);
    }
}
