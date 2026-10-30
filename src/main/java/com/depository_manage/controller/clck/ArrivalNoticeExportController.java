package com.depository_manage.controller.clck;

import com.depository_manage.pojo.ArrivalNoticeRow;
import com.depository_manage.service.clck.ShipmentDetailsService;
import com.depository_manage.utils.ArrivalNoticeExcelExporter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/shipment/arrival-notice")
public class ArrivalNoticeExportController {
    private final ShipmentDetailsService shipmentDetailsService;

    public ArrivalNoticeExportController(ShipmentDetailsService shipmentDetailsService) {
        this.shipmentDetailsService = shipmentDetailsService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonthlyNotice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate) throws IOException {
        List<ArrivalNoticeRow> rows = shipmentDetailsService.getMonthlyArrivalNotice(arrivalDate);
        if (rows.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        YearMonth month = YearMonth.from(arrivalDate);
        String filename = "原材料到货通知_" + month + "_明细.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("attachment")
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(ArrivalNoticeExcelExporter.export(rows, month));
    }
}
