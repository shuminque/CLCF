package com.depository_manage.controller;

import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.clck.DailyCounterService;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// Import the necessary Spring annotations and types
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private ShipmentDetailsService shipmentDetailsService;
    @Autowired
    private DailyCounterService dailyCounterService;
    @PostMapping
    public ResponseEntity<List<ShipmentDetails>> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam("invoiceNumber") String invoiceNumber,
                                                                  @RequestParam("customer") String customer,
                                                                  @RequestParam("tradeMode") String tradeMode,
                                                                  @RequestParam("deliveryPoint") String deliveryPoint,
                                                                  @RequestParam("purchaser") String purchaser,
                @RequestParam("arrivalPortDate") @DateTimeFormat(pattern="yyyy-MM-dd") Date arrivalPortDate,
                @RequestParam("arrivalDate") @DateTimeFormat(pattern="yyyy-MM-dd") Date arrivalDate,
                                                                  @RequestParam("steelGrade") String steelGrade,
                                                                  @RequestParam("steelType") String steelType,
                                                                  @RequestParam("steelSize") String steelSize){
        List<ShipmentDetails> shipments = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook;
            if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (file.getOriginalFilename().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Bad request if file type is unsupported
            }
            Sheet sheet = workbook.getSheetAt(0); // Assuming there is at least one sheet
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                ShipmentDetails shipment = parseShipmentDetails(row,invoiceNumber, customer,tradeMode,deliveryPoint,purchaser,
                        arrivalPortDate,arrivalDate,steelGrade,steelType,steelSize);
                if (shipment != null) {
                    LocalDate today = LocalDate.now();
                    int counter = dailyCounterService.findAndUpdateCounterByDate(today); // 查询并更新序号
                    String index = String.valueOf(counter);
                    String uniqueIdentifier = today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + index; // 生成唯一标识符
                    shipment.setUniqueIdentifier(uniqueIdentifier);
                    shipments.add(shipment);
                }
            }
            List<ShipmentDetails> savedShipments = shipmentDetailsService.saveAll(shipments);
            return ResponseEntity.ok(savedShipments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private ShipmentDetails parseShipmentDetails(Row row, String invoiceNumber, String customer, String tradeMode,
                                                 String deliveryPoint, String purchaser,Date arrivalPortDate, Date arrivalDate,
                                                 String steelGrade, String steelType, String steelSize) {
        if (row == null) {
            return null;
        }
        ShipmentDetails shipment = new ShipmentDetails();
        int filledFieldsCount = 0;
            shipment.setInvoiceNumber(invoiceNumber);
            shipment.setCustomer(customer);
            shipment.setTradeMode(tradeMode);
            shipment.setDeliveryPoint(deliveryPoint);
            shipment.setPurchaser(purchaser);
            shipment.setArrivalPortDate(arrivalPortDate);
            shipment.setArrivalDate(arrivalDate);

            shipment.setSteelGrade(steelGrade);
            filledFieldsCount++;

            shipment.setDimensions(steelSize);
            filledFieldsCount++;

            shipment.setSteelMill(steelType);
            filledFieldsCount++;
        if (getCellValueAsDouble(row.getCell(3)) != null) {
            shipment.setWeight(getCellValueAsDouble(row.getCell(3)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(4)) != null) {
            shipment.setFurnaceNumber(getCellValueAsString(row.getCell(4)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(5)) != null) {
            shipment.setSupplierBatchNumber(getCellValueAsString(row.getCell(5)));
            filledFieldsCount++;
        }

        if (getCellValueAsInteger(row.getCell(6)) != null) {
            shipment.setBundleCount(getCellValueAsInteger(row.getCell(6)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(13)) != null) {
            shipment.setInvoiceApplication(getCellValueAsString(row.getCell(13)));
            filledFieldsCount++;
        }
        if (filledFieldsCount < 7) {
            return null;
        }

        return shipment;
    }

    private String getCellValueAsString(Cell cell) {
        try {
            return cell == null || cell.getCellType() == CellType.BLANK ? null : cell.getStringCellValue();
        } catch (Exception e) {
            return null;  // Return null if there is any exception, which includes type mismatches
        }
    }

    private Date getCellValueAsDate(Cell cell) {
        try {
            return cell == null || (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA) ? null : cell.getDateCellValue();
        } catch (Exception e) {
            return null;  // Return null if there is any exception
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                return null;
            } else if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
                return cell.getNumericCellValue();
            } else {
                return null;  // Return null if the cell value is not a numeric or formula
            }
        } catch (Exception e) {
            return null;  // Return null if there is any exception
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        try {
            return cell == null || cell.getCellType() == CellType.BLANK ? null : (int) cell.getNumericCellValue();
        } catch (Exception e) {
            return null;  // Return null if there is any exception
        }
    }

}
