package com.depository_manage;

import com.depository_manage.controller.PageController;
import com.depository_manage.controller.clck.ShipmentDetailsController;
import com.depository_manage.mapper.clck.ShipmentDetailsMapper;
import com.depository_manage.pojo.ArrivalNoticeRow;
import com.depository_manage.service.clck.ShipmentDetailsService;
import com.depository_manage.service.clck.impl.ShipmentDetailsServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ArrivalNoticeTest {
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
                    "furnace_number VARCHAR(100), weight DOUBLE, bundle_count INT, operation_type VARCHAR(100))");
        }
        service = new ShipmentDetailsServiceImpl(session.getMapper(ShipmentDetailsMapper.class));
        nextId = 0;
    }

    @After
    public void closeMapper() {
        if (session != null) session.close();
    }

    private void insert(String uid, String date, String point, String trade, String customer,
                        String size, String grade, String mill, String furnace, Double weight,
                        int pieces, String operation) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO shipment_details VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            Object[] values = {++nextId, uid, date, point, trade, customer, size, grade, mill, furnace,
                    weight, pieces, operation};
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private void insertSample(String uid, String date, String customer, String furnace,
                              double weight, String operation) throws Exception {
        insert(uid, date, "ZAB", "一般贸易", customer, "30.40", "S2", "江阴", furnace,
                weight, 120, operation);
    }

    @Test
    public void mergesSampleInto32And5BundlesWithoutCountingMovementCopies() throws Exception {
        for (int i = 0; i < 32; i++) {
            insertSample("KS-" + i, "2026-07-28", "新(KS)", "E12605569XX",
                    i == 31 ? 2392 : 900, i == 0 ? "入库" : null);
        }
        for (int i = 0; i < 5; i++) {
            insertSample("HF-" + i, "2026-07-28", "新(HF)", "E12604384XX", 844.4, null);
        }
        insertSample("KS-0", "2026-07-28", "新(KS)", "E12605569XX", 900, "出库");
        insertSample("KS-0", "2026-07-28", "新(KS)", "E12605569XX", 900, "返库");
        insertSample("other-day", "2026-07-29", "新(KS)", "E12605569XX", 99999, null);
        List<ArrivalNoticeRow> rows = service.getArrivalNotice("2026-07-28");
        assertEquals(2, rows.size());
        ArrivalNoticeRow ks = rows.stream().filter(row -> row.getCustomer().equals("新(KS)")).findFirst().get();
        ArrivalNoticeRow hf = rows.stream().filter(row -> row.getCustomer().equals("新(HF)")).findFirst().get();
        assertEquals(Long.valueOf(32), ks.getBundleCount());
        assertEquals(0, new BigDecimal("30292").compareTo(ks.getWeight()));
        assertEquals(Long.valueOf(5), hf.getBundleCount());
        assertEquals(0, new BigDecimal("4222").compareTo(hf.getWeight()));
        assertEquals("2026-07-28", ks.getArrivalDate());
        assertEquals("E12605569XX", ks.getFurnaceNumber());
    }

    @Test
    public void combinesFurnaceNumbersForMatchingPrintedFields() throws Exception {
        String[] fields = {"ZAB", "一般贸易", "客户原名", "30.40", "S2", "江阴", "炉号A"};
        insert("base", "2026-07-28", fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], 10.0, 50, null);
        for (int i = 0; i < fields.length - 1; i++) {
            String[] changed = fields.clone();
            changed[i] += "另一个";
            insert("variant-" + i, "2026-07-28", changed[0], changed[1], changed[2], changed[3],
                    changed[4], changed[5], changed[6], 10.0, 50, null);
        }
        insert("another-furnace", "2026-07-28", fields[0], fields[1], fields[2], fields[3],
                fields[4], fields[5], "炉号B", 15.0, 50, null);
        insert("duplicate-furnace", "2026-07-28", fields[0], fields[1], fields[2], fields[3],
                fields[4], fields[5], fields[6], 5.0, 50, null);
        List<ArrivalNoticeRow> rows = service.getArrivalNotice("2026-07-28");
        assertEquals(7, rows.size());
        ArrivalNoticeRow merged = rows.stream()
                .filter(row -> row.getCustomer().equals("客户原名") && row.getFurnaceNumber().contains("炉号B"))
                .findFirst().get();
        assertEquals(Long.valueOf(3), merged.getBundleCount());
        assertEquals(0, new BigDecimal("30").compareTo(merged.getWeight()));
        assertEquals("炉号A/炉号B", merged.getFurnaceNumber());
        for (ArrivalNoticeRow row : rows.stream().filter(row -> row != merged).toArray(ArrivalNoticeRow[]::new)) {
            assertEquals(Long.valueOf(1), row.getBundleCount());
        }
        assertTrue(service.getArrivalNotice("2026-07-27").isEmpty());
    }

    @Test
    public void countsLegacyRecordsIndividuallyAndSumsFractionalWeightsExactly() throws Exception {
        insertSample(null, "2026-07-28", "原客户", "炉号", 0.1, null);
        insertSample(null, "2026-07-28", "原客户", "炉号", 0.2, null);
        insertSample("", "2026-07-28", "原客户", "炉号", 0, null);
        insertSample("", "2026-07-28", "原客户", "炉号", 0, null);
        ArrivalNoticeRow row = service.getArrivalNotice("2026-07-28").get(0);
        assertEquals(Long.valueOf(4), row.getBundleCount());
        assertEquals(0, new BigDecimal("0.3").compareTo(row.getWeight()));
    }

    @Test
    public void endpointRequiresValidDateAndReturnsPrintableFields() throws Exception {
        ShipmentDetailsService mockedService = mock(ShipmentDetailsService.class);
        ArrivalNoticeRow row = new ArrivalNoticeRow();
        row.setCustomer("客户原名");
        row.setBundleCount(32L);
        row.setWeight(new BigDecimal("30292"));
        row.setFurnaceNumber("E12605569XX");
        when(mockedService.getArrivalNotice("2026-07-28")).thenReturn(Collections.singletonList(row));
        ShipmentDetailsController controller = new ShipmentDetailsController();
        ReflectionTestUtils.setField(controller, "shipmentDetailsService", mockedService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/shipment/arrival-notice").param("arrivalDate", "2026-07-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customer").value("客户原名"))
                .andExpect(jsonPath("$[0].bundleCount").value(32))
                .andExpect(jsonPath("$[0].weight").value(30292))
                .andExpect(jsonPath("$[0].furnaceNumber").value("E12605569XX"));
        mvc.perform(get("/shipment/arrival-notice")).andExpect(status().isBadRequest());
        mvc.perform(get("/shipment/arrival-notice").param("arrivalDate", "invalid")).andExpect(status().isBadRequest());
        verify(mockedService).getArrivalNotice("2026-07-28");
        verifyNoMoreInteractions(mockedService);
        MockMvcBuilders.standaloneSetup(new PageController()).build()
                .perform(get("/arrivalNoticePrint")).andExpect(view().name("clck/ArrivalNoticePrint"));
    }
}
