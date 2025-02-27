package com.depository_manage.controller.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.BearingRecordService;
import com.depository_manage.service.clck.DailyCounterService;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/shipment")
public class ShipmentDetailsController {

    @Autowired
    private ShipmentDetailsService shipmentDetailsService;

    @Autowired
    private BearingRecordService bearingRecordService;

    @Autowired
    private DailyCounterService dailyCounterService;

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
        String dateRange2 = (String) params.get("time2");
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] dates2 = dateRange.split(" - ");
            params.put("startDate2", dates2[0] + " 00:00:00");
            params.put("endDate2", dates2[1] + " 23:59:59");
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
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteShipmentDetail(@PathVariable int id) {
        boolean isDeleted = shipmentDetailsService.deleteShipmentDetailById(id);
        if (isDeleted) {
            return new ResponseEntity<>("Shipment detail deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Failed to delete shipment detail", HttpStatus.NOT_FOUND);
        }
    }
    @PostMapping("/batchDelete")
    public ResponseEntity<String> batchDeleteShipmentDetails(@RequestBody List<Integer> ids) {
        boolean isDeleted = shipmentDetailsService.batchDeleteShipmentDetails(ids);
        if (isDeleted) {
            return new ResponseEntity<>("Shipment details deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Failed to delete shipment details", HttpStatus.NOT_FOUND);
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
    @PostMapping("/tuiHuo")
    public ResponseEntity<Void> tuiHuo(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            shipmentDetailsService.tuiHuo(uniqueIdentifier);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if stock out is not allowed
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
            // 调用服务层的 transfer 方法，并返回消息
            String resultMessage = shipmentDetailsService.transfer(uniqueIdentifier, placementArea);
            return ResponseEntity.ok(resultMessage);  // 返回提示信息
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());  // 返回 400 错误并带上错误信息
        }
    }


    @PostMapping("/return")
    public ResponseEntity<Void> returnToStock(@RequestBody Map<String, String> request) {
        try {
            String uniqueIdentifier = request.get("uniqueIdentifier");
            double weight = Double.parseDouble(request.get("weight"));
            String placementArea = request.get("placementArea");
            shipmentDetailsService.returnToStock(uniqueIdentifier, weight ,placementArea);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if return operation is not allowed
        }
    }
    @PostMapping("/returnPC")
    public ResponseEntity<List<ShipmentDetails>> returnPC(@RequestBody Map<String, String> request) {
        try {
            List<ShipmentDetails> shipments = new ArrayList<>();
            ShipmentDetails shipment = new ShipmentDetails();
            shipment.setSteelMill(request.get("steelType"));
            shipment.setSteelGrade(request.get("steelGrade"));
            shipment.setDimensions(request.get("steelSize"));
            shipment.setTradeMode(request.get("tradeMode"));
            shipment.setFurnaceNumber(request.get("furnaceNumber"));
            shipment.setWeight(Double.valueOf(request.get("weight")));
            shipment.setPlacementArea(request.get("area"));
            shipment.setState("正常");
            ShipmentDetails lastShipment = shipmentDetailsService.getLastCustomer(
                    request.get("steelType"),
                    request.get("steelGrade"),
                    request.get("steelSize"),
                    request.get("tradeMode")
            );
            if (lastShipment != null) {
                shipment.setCustomer(lastShipment.getCustomer());
                shipment.setDeliveryPoint(lastShipment.getDeliveryPoint());
                shipment.setPurchaser(lastShipment.getPurchaser());
            }
            String dateString = request.get("date");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dateFormat.parse(dateString);
            shipment.setTime(date);
            shipment.setArrivalDate(date);
            LocalDate today = LocalDate.now();
            int counter = dailyCounterService.findAndUpdateCounterByDate(today); // 查询并更新序号
            String index = String.valueOf(counter);
            String uniqueIdentifier = today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + index; // 生成唯一标识符
            shipment.setOperationType("返库");
            shipment.setUniqueIdentifier(uniqueIdentifier);
            shipments.add(shipment);
            List<ShipmentDetails> savedShipments = shipmentDetailsService.saveAll(shipments);
            return ResponseEntity.ok(savedShipments);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if return operation is not allowed
        }
    }
    @GetMapping("/getStockStatusBeforeCutoffDate")
    public ResponseEntity<?> getStockStatusBeforeCutoffDate(@RequestParam Map<String, Object> params) {
        System.out.println(params);
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
            List<Map<String, String>> result = bearingRecordService.getAllWEs(params);
            for (Map<String, String> row : result) {
                String steel_mill= row.get("steel_type");
                String steel_grade = row.get("steel_grade");
                String dimensions= row.get("size");
                String trade_mode = row.get("mode");
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
                List<ShipmentDetails> records0 = shipmentDetailsService.getIntoSDs(params2);
                if (!records0.isEmpty()) {
                    ShipmentDetails firstRecord = records0.get(0);
                    row.put("weight", String.valueOf(firstRecord.getWeight()));
                    row.put("bundle_count", String.valueOf(firstRecord.getBundleCount()));
                } else {
                    row.put("weight", "0");
                    row.put("bundle_count", "0");
                }
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

    @GetMapping("/clstatus_steel_mill")
    public ResponseEntity<List<Map<String, Object>>> getclstatus(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date cutoffDate) {
        List<Map<String, Object>> inventoryStatus = shipmentDetailsService.getclstatus(cutoffDate);
        return ResponseEntity.ok(inventoryStatus);
    }
    @GetMapping("/clstatus_dimensions")
    public ResponseEntity<List<Map<String, Object>>> getClstatusByDimensions(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date cutoffDate) {
        List<Map<String, Object>> inventoryStatus = shipmentDetailsService.getClstatusByDimensions(cutoffDate);
        return ResponseEntity.ok(inventoryStatus);
    }
    @GetMapping("/pipe_dimensions")
    public ResponseEntity<List<Map<String, Object>>> getPipeByDimensions(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date cutoffDate) {
        List<Map<String, Object>> inventoryStatus = shipmentDetailsService.getPipeByDimensions(cutoffDate);
        return ResponseEntity.ok(inventoryStatus);
    }
    @GetMapping("/forg_dimensions")
    public ResponseEntity<List<Map<String, Object>>> getForgByDimensions(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date cutoffDate) {
        List<Map<String, Object>> inventoryStatus = shipmentDetailsService.getForgByDimensions(cutoffDate);
        return ResponseEntity.ok(inventoryStatus);
    }
    @GetMapping("/monthlyInOutWeightByYear")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyInOutWeightByYear(@RequestParam int year) {
        List<Map<String, Object>> monthlyInOutWeight = shipmentDetailsService.getMonthlyInOutWeightByYear(year);
        return ResponseEntity.ok(monthlyInOutWeight);
    }
    @GetMapping("/monthlyCumulativeInventoryStatusByYear")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyCumulativeInventoryStatusByYear(@RequestParam int year) {
        List<Map<String, Object>> monthlyInventoryStatus = shipmentDetailsService.getMonthlyCumulativeInventoryStatusByYear(year);
        return ResponseEntity.ok(monthlyInventoryStatus);
    }
    @GetMapping("/query")
    public ResponseEntity<?> queryShipmentDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String endDate,
            @RequestParam Map<String, Object> params) {
        if (startDate != null && endDate != null) {
            params.put("startDate", startDate + " 00:00:00");
            params.put("endDate", endDate + " 23:59:59");
        }

        List<Map<String, Object>> result = shipmentDetailsService.queryShipmentDetails(params);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "");
        response.put("count", result.size());
        response.put("data", result);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/updateInvoice")
    public Map<String, Object> updateInvoiceApplication(
            @RequestParam String arrival_date,
            @RequestParam String steel_mill,
            @RequestParam String steel_grade,
            @RequestParam String dimensions,
            @RequestParam String trade_mode,
            @RequestParam String invoice_application,
            @RequestParam(required = false) BigDecimal original_unit_price,  // 旧单价
            @RequestParam(required = false) BigDecimal new_unit_price) {    // 新单价
        Map<String, Object> response = new HashMap<>();
        try {
            // 调用服务层更新结算状态和单价
            shipmentDetailsService.updateInvoiceApplication(arrival_date, steel_mill, steel_grade, dimensions, trade_mode, invoice_application, original_unit_price, new_unit_price);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }


    @GetMapping("/selectInvoiceApplication")
    public ResponseEntity<List<Map<String, Object>>> selectInvoiceApplication(
            @RequestParam String arrival_date,
            @RequestParam String steel_mill,
            @RequestParam String steel_grade,
            @RequestParam String dimensions,
            @RequestParam String trade_mode,
            @RequestParam double unit_price) {

        Map<String, Object> params = new HashMap<>();
        params.put("arrival_date", arrival_date);
        params.put("steel_mill", steel_mill);
        params.put("steel_grade", steel_grade);
        params.put("dimensions", dimensions);
        params.put("trade_mode", trade_mode);
        params.put("unit_price", unit_price);
        System.out.println(params);
        List<Map<String, Object>> result = shipmentDetailsService.selectInvoiceApplication(params);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/updateUnitPrice")
    public ResponseEntity<?> updateUnitPrice(
            @RequestParam String unique_identifier,
            @RequestParam double unit_price) {

        try {
            shipmentDetailsService.updateUnitPrice(unique_identifier, unit_price);
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonMap("success", false));
        }
    }

    @GetMapping("/details")
    public ResponseEntity<?> getShipmentDetails(@RequestParam Map<String, Object> params) {
        System.out.println(params);
        String dateRange = (String) params.get("time");
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] dates = dateRange.split(" - ");
            params.put("startDate", dates[0] + " 00:00:00");
            params.put("endDate", dates[1] + " 23:59:59");
        }

        List<Map<String, Object>> results = shipmentDetailsService.fetchShipmentDetails(params);
        return ResponseEntity.ok(results);
    }
    @GetMapping("/monthlyInventory")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyInventory(
            @RequestParam(required = false, defaultValue = "正常") String state,
            @RequestParam(required = false) String year) {
        // 如果年份未指定，默认为当前年份
        if (year == null || year.isEmpty()) {
            year = String.valueOf(LocalDate.now().getYear());
        }
        List<Map<String, Object>> inventoryStatus = shipmentDetailsService.getMonthlyInventory(state, year);
        return ResponseEntity.ok(inventoryStatus);
    }
}
