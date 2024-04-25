package com.depository_manage.controller;

import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.service.ShipmentDetailsService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private ShipmentDetailsService shipmentDetailsService;

    @PostMapping
    public ResponseEntity<List<ShipmentDetails>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        List<ShipmentDetails> shipments = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook;
            if (file.getOriginalFilename().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (file.getOriginalFilename().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Bad request if file type is unsupported
            }

            Sheet sheet = workbook.getSheetAt(0); // Assuming there is at least one sheet
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                ShipmentDetails shipment = parseShipmentDetails(row);
                if (shipment != null) {
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
    private ShipmentDetails parseShipmentDetails(Row row) {
        if (row == null) {
            return null;
        }
        ShipmentDetails shipment = new ShipmentDetails();
        int filledFieldsCount = 0;

        if (getCellValueAsString(row.getCell(0)) != null) {
            shipment.setInvoiceNumber(getCellValueAsString(row.getCell(0)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(1)) != null) {
            shipment.setCustomer(getCellValueAsString(row.getCell(1)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(8)) != null) {
            shipment.setTradeMode(getCellValueAsString(row.getCell(8)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(0)) != null) {
            shipment.setDeliveryPoint(getCellValueAsString(row.getCell(0)));
            filledFieldsCount++;
        }
        if (getCellValueAsDate(row.getCell(8)) != null) {
            shipment.setArrivalPortDate(getCellValueAsDate(row.getCell(8)));
            shipment.setArrivalDate(getCellValueAsDate(row.getCell(8)));
            filledFieldsCount++; // Count as one since both dates are the same
        }
        if (getCellValueAsString(row.getCell(3)) != null) {
            shipment.setDimensions(getCellValueAsString(row.getCell(3)));
            filledFieldsCount++;
        }
        if (getCellValueAsDouble(row.getCell(6)) != null) {
            shipment.setWeight(getCellValueAsDouble(row.getCell(6)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(2)) != null) {
            shipment.setSteelMill(getCellValueAsString(row.getCell(2)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(5)) != null) {
            shipment.setFurnaceNumber(getCellValueAsString(row.getCell(5)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(10)) != null) {
            shipment.setInvoiceApplication(getCellValueAsString(row.getCell(10)));
            filledFieldsCount++;
        }

            shipment.setOperationType("入库");
            filledFieldsCount++;

        if (getCellValueAsString(row.getCell(4)) != null) {
            shipment.setSupplierBatchNumber(getCellValueAsString(row.getCell(4)));
            filledFieldsCount++;
        }
        if (getCellValueAsInteger(row.getCell(7)) != null) {
            shipment.setBundleCount(getCellValueAsInteger(row.getCell(7)));
            filledFieldsCount++;
        }
        if (getCellValueAsString(row.getCell(12)) != null) {
            shipment.setPlacementArea(getCellValueAsString(row.getCell(12)));
            filledFieldsCount++;
        }
        if (filledFieldsCount < 6) {
            return null; // Not enough fields filled to consider this a valid record
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
