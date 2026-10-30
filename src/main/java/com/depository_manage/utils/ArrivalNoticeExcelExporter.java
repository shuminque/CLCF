package com.depository_manage.utils;

import com.depository_manage.pojo.ArrivalNoticeRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

public final class ArrivalNoticeExcelExporter {
    private ArrivalNoticeExcelExporter() {
    }

    public static byte[] export(List<ArrivalNoticeRow> rows, YearMonth month) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("到货明细");
            String[] headers = {"序号", "到货日", "到货点", "贸易方式", "顾客名称", "规格",
                    "捆数", "KGS/PCS", "钢厂", "备注（炉号）"};
            int[] widths = {8, 14, 12, 16, 24, 22, 10, 18, 22, 48};
            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle heading = workbook.createCellStyle();
            heading.setFont(bold);
            heading.setAlignment(HorizontalAlignment.CENTER);
            heading.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle weight = workbook.createCellStyle();
            weight.setDataFormat(workbook.createDataFormat().getFormat("#,##0.######"));

            Row title = sheet.createRow(0);
            title.setHeightInPoints(28);
            title.createCell(0).setCellValue(month.getYear() + "年" + month.getMonthValue() + "月原材料到货通知明细");
            title.getCell(0).setCellStyle(heading);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
            Row header = sheet.createRow(1);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(heading);
                sheet.setColumnWidth(column, widths[column] * 256);
            }
            for (int index = 0; index < rows.size(); index++) {
                ArrivalNoticeRow notice = rows.get(index);
                Row row = sheet.createRow(index + 2);
                row.createCell(0).setCellValue(index + 1);
                row.createCell(1).setCellValue(text(notice.getArrivalDate()));
                row.createCell(2).setCellValue(text(notice.getDeliveryPoint()));
                row.createCell(3).setCellValue(text(notice.getTradeMode()));
                row.createCell(4).setCellValue(text(notice.getCustomer()));
                row.createCell(5).setCellValue(text(notice.getDimensions()) + text(notice.getSteelGrade()));
                Cell bundles = row.createCell(6);
                if (notice.getBundleCount() != null) bundles.setCellValue(notice.getBundleCount());
                Cell kilograms = row.createCell(7);
                if (notice.getWeight() != null) kilograms.setCellValue(notice.getWeight().doubleValue());
                kilograms.setCellStyle(weight);
                row.createCell(8).setCellValue(text(notice.getSteelMill()));
                row.createCell(9).setCellValue(text(notice.getFurnaceNumber()));
            }
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new CellRangeAddress(1, rows.size() + 1, 0, headers.length - 1));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
