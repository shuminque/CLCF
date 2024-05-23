package com.depository_manage.controller.clck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        public ResponseEntity<List<ShipmentDetails>> getAllRecords() {
            List<ShipmentDetails> records = shipmentDetailsService.getAllShipmentDetails();
            return ResponseEntity.ok(records);
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
            String supplierBatchNumber = request.get("supplierBatchNumber");
            String operationType = request.get("operationType");
            String placementArea = request.get("placementArea");
            shipmentDetailsService.updateOperationTypeBySupplierBatchNumber(supplierBatchNumber, operationType, placementArea);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if duplicate entry is attempted
        }
    }
    @PostMapping("/stockOut")
    public ResponseEntity<Void> stockOut(@RequestBody Map<String, String> request) {
        try {
            String supplierBatchNumber = request.get("supplierBatchNumber");
            shipmentDetailsService.stockOut(supplierBatchNumber);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);  // Return 400 Bad Request if stock out is not allowed
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody Map<String, String> request) {
        String supplierBatchNumber = request.get("supplierBatchNumber");
        String placementArea = request.get("placementArea");
        try {
            shipmentDetailsService.transfer(supplierBatchNumber, placementArea);
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

}
