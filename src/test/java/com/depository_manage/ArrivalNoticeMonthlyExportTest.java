package com.depository_manage;

import com.depository_manage.controller.clck.ArrivalNoticeExportController;
import com.depository_manage.mapper.clck.ShipmentDetailsMapper;
import com.depository_manage.pojo.ArrivalNoticeRow;
import com.depository_manage.service.clck.ShipmentDetailsService;
import com.depository_manage.service.clck.impl.ShipmentDetailsServiceImpl;
import com.depository_manage.utils.ArrivalNoticeExcelExporter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ArrivalNoticeMonthlyExportTest {
    private SqlSession session;
    private ShipmentDetailsService service;
    private int nextId;

    @Before
    public void setupMapper() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        Configuration configuration = new Configuration(new Environment("test",
                new JdbcTransactionFactory(), dataSource));
        String resource = "mapper/clck/ShipmentDetailsMapper.xml";
        try (InputStream stream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE shipment_details (id INT PRIMARY KEY, unique_identifier VARCHAR(100), " +
                    "arrival_date DATE, delivery_point VARCHAR(100), trade_mode VARCHAR(100), customer VARCHAR(100), " +
                    "dimensions VARCHAR(100), steel_grade VARCHAR(100), steel_mill VARCHAR(100), " +
                    "furnace_number VARCHAR(100), weight DECIMAL(24,6), operation_type VARCHAR(100))");
        }
        service = new ShipmentDetailsServiceImpl(session.getMapper(ShipmentDetailsMapper.class));
        nextId = 0;
    }

    @After
    public void closeMapper() {
        if (session != null) session.close();
    }

    private void insert(String uid, String date, String furnace, String weight, String operation) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO shipment_details VALUES (?, ?, ?, 'ZAB', '一般贸易', '客户原名', '030.40', 'S2', '江阴', ?, ?, ?)")) {
            statement.setInt(1, ++nextId);
            statement.setString(2, uid);
            statement.setString(3, date);
            statement.setString(4, furnace);
            statement.setBigDecimal(5, new BigDecimal(weight));
            statement.setString(6, operation);
            statement.executeUpdate();
        }
    }

    private void insertOctober() throws Exception {
        // Deliberately insert out of date order and include copies of the same material.
        insert("last", "2026-10-31", "炉号末", "31", null);
        insert("first-a", "2026-10-01", "炉号A", "0.1", "入库");
        insert("first-b", "2026-10-01", "炉号B", "0.2", null);
        insert("first-a", "2026-10-01", "炉号A", "0.1", "出库");
        insert("first-a", "2026-10-01", "炉号A", "0.1", "返库");
        insert("middle", "2026-10-25", "炉号中", "25.125", null);
        insert("previous", "2026-09-30", "不应导出", "999", null);
        insert("next", "2026-11-01", "不应导出", "999", null);
        insert("previous-year", "2025-10-25", "不应导出", "999", null);
    }

    @Test
    public void selectedDayExportsWholeMonthInDateOrderWithDailyGroupingAndDeduplication() throws Exception {
        insertOctober();
        List<ArrivalNoticeRow> rows = service.getMonthlyArrivalNotice(LocalDate.of(2026, 10, 25));
        assertEquals(3, rows.size());
        assertEquals("2026-10-01,2026-10-25,2026-10-31", rows.stream()
                .map(ArrivalNoticeRow::getArrivalDate).collect(Collectors.joining(",")));
        assertEquals(Long.valueOf(2), rows.get(0).getBundleCount());
        assertEquals(0, new BigDecimal("0.3").compareTo(rows.get(0).getWeight()));
        assertEquals("炉号A/炉号B", rows.get(0).getFurnaceNumber());
        assertEquals(Long.valueOf(1), rows.get(1).getBundleCount());
        assertEquals(0, new BigDecimal("25.125").compareTo(rows.get(1).getWeight()));
        assertEquals(1, service.getArrivalNotice("2026-10-25").size());
        assertEquals(3, service.getMonthlyArrivalNotice(LocalDate.of(2026, 10, 20)).size());
        assertTrue(service.getArrivalNotice("2026-10-20").isEmpty());
    }

    @Test
    public void monthBoundariesHandleDecemberAndLeapFebruary() throws Exception {
        insert("december", "2026-12-31", "炉号", "1", null);
        insert("january", "2027-01-01", "炉号", "2", null);
        insert("leap", "2024-02-29", "炉号", "3", null);
        insert("march", "2024-03-01", "炉号", "4", null);
        List<ArrivalNoticeRow> december = service.getMonthlyArrivalNotice(LocalDate.of(2026, 12, 25));
        assertEquals(1, december.size());
        assertEquals("2026-12-31", december.get(0).getArrivalDate());
        List<ArrivalNoticeRow> february = service.getMonthlyArrivalNotice(LocalDate.of(2024, 2, 25));
        assertEquals(1, february.size());
        assertEquals("2024-02-29", february.get(0).getArrivalDate());
    }

    @Test
    public void downloadContainsTheEntireMonthWithReadableHeadersAndNumericQuantities() throws Exception {
        insertOctober();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ArrivalNoticeExportController(service)).build();
        MvcResult result = mvc.perform(get("/shipment/arrival-notice/export").param("arrivalDate", "2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        assertEquals("原材料到货通知_2026-10_明细.xlsx", ContentDisposition.parse(
                result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheet("到货明细");
            assertEquals(4, sheet.getLastRowNum());
            assertEquals("2026年10月原材料到货通知明细", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("到货日", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("2026-10-01", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("2026-10-25", sheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("2026-10-31", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("030.40S2", sheet.getRow(2).getCell(5).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(2).getCell(6).getCellType());
            assertEquals(2, sheet.getRow(2).getCell(6).getNumericCellValue(), 0);
            assertEquals(0.3, sheet.getRow(2).getCell(7).getNumericCellValue(), 0.000001);
            assertEquals("炉号A/炉号B", sheet.getRow(2).getCell(9).getStringCellValue());
            assertTrue(sheet.getPaneInformation().isFreezePane());
        }
    }

    @Test
    public void missingOrInvalidDatesAreRejectedAndEmptyMonthsReturnNoDownload() throws Exception {
        ShipmentDetailsService mockedService = mock(ShipmentDetailsService.class);
        when(mockedService.getMonthlyArrivalNotice(LocalDate.of(2026, 10, 25))).thenReturn(Collections.emptyList());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ArrivalNoticeExportController(mockedService)).build();
        mvc.perform(get("/shipment/arrival-notice/export")).andExpect(status().isBadRequest());
        mvc.perform(get("/shipment/arrival-notice/export").param("arrivalDate", "invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/shipment/arrival-notice/export").param("arrivalDate", "2026-02-30"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mockedService);
        mvc.perform(get("/shipment/arrival-notice/export").param("arrivalDate", "2026-10-25"))
                .andExpect(status().isNoContent()).andExpect(content().bytes(new byte[0]));
        verify(mockedService).getMonthlyArrivalNotice(LocalDate.of(2026, 10, 25));
        verifyNoMoreInteractions(mockedService);
    }

    @Test
    public void blankFieldsStayBlankAndImportedTextIsNotAnExcelFormula() throws Exception {
        ArrivalNoticeRow row = new ArrivalNoticeRow();
        row.setArrivalDate("2026-10-25");
        row.setCustomer("=2+2");
        byte[] bytes = ArrivalNoticeExcelExporter.export(Collections.singletonList(row), YearMonth.of(2026, 10));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(CellType.STRING, sheet.getRow(2).getCell(4).getCellType());
            assertEquals("=2+2", sheet.getRow(2).getCell(4).getStringCellValue());
            assertEquals("", sheet.getRow(2).getCell(5).getStringCellValue());
            assertEquals(CellType.BLANK, sheet.getRow(2).getCell(6).getCellType());
            assertEquals("", sheet.getRow(2).getCell(9).getStringCellValue());
        }
    }
}
