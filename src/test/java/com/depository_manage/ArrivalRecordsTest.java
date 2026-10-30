package com.depository_manage;

import com.depository_manage.controller.clck.ShipmentDetailsController;
import com.depository_manage.entity.ShipmentDetails;
import com.depository_manage.mapper.clck.ShipmentDetailsMapper;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ArrivalRecordsTest {
    private SqlSession session;
    private ShipmentDetailsService service;
    private MockMvc mvc;
    private int nextId;

    @Before
    public void setup() throws Exception {
        UnpooledDataSource source = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
        String resource = "mapper/clck/ShipmentDetailsMapper.xml";
        try (InputStream stream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(config).openSession();
        try (Statement sql = session.getConnection().createStatement()) {
            sql.execute("CREATE TABLE shipment_details (id INT PRIMARY KEY, unique_identifier VARCHAR(100), " +
                    "invoice_number VARCHAR(100) DEFAULT 'INV', customer VARCHAR(100) DEFAULT '客户甲', " +
                    "trade_mode VARCHAR(100) DEFAULT '一般贸易', delivery_point VARCHAR(100) DEFAULT 'ZAB', " +
                    "arrival_port_date DATE, arrival_date TIMESTAMP, steel_grade VARCHAR(100) DEFAULT '3B', " +
                    "dimensions VARCHAR(100) DEFAULT '32.35', weight DOUBLE, " +
                    "steel_mill VARCHAR(100) DEFAULT '江阴材', furnace_number VARCHAR(100) DEFAULT 'F123', " +
                    "invoice_application VARCHAR(100), operation_type VARCHAR(100), " +
                    "supplier_batch_number VARCHAR(100) DEFAULT 'B123', bundle_count INT DEFAULT 20, " +
                    "placement_area VARCHAR(100) DEFAULT 'C10区', purchaser VARCHAR(100) DEFAULT '购买方甲', " +
                    "`time` TIMESTAMP DEFAULT '2026-10-26 00:00:00', unit_price DECIMAL(12,5), " +
                    "state VARCHAR(100) DEFAULT '正常')");
        }
        service = new ShipmentDetailsServiceImpl(session.getMapper(ShipmentDetailsMapper.class));
        ShipmentDetailsController controller = new ShipmentDetailsController();
        ReflectionTestUtils.setField(controller, "shipmentDetailsService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        nextId = 0;
    }

    @After
    public void close() {
        if (session != null) session.close();
    }

    private int insert(String uid, String operation, double weight, String date) throws Exception {
        int id = ++nextId;
        try (PreparedStatement sql = session.getConnection().prepareStatement(
                "INSERT INTO shipment_details (id, unique_identifier, operation_type, weight, arrival_date) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
            sql.setInt(1, id);
            sql.setString(2, uid);
            sql.setString(3, operation);
            sql.setDouble(4, weight);
            sql.setString(5, date);
            sql.executeUpdate();
        }
        return id;
    }

    private int insert(String uid, String operation, double weight) throws Exception {
        return insert(uid, operation, weight, "2026-10-19");
    }

    private List<Integer> ids(Map<String, Object> params) {
        return service.getArrivalRecords(params).stream().map(ShipmentDetails::getId).collect(Collectors.toList());
    }

    @Test
    public void includesInboundAndBothUnoperatedFormsOnceAndLeavesFlowQueryUnchanged() throws Exception {
        int inbound = insert("M1", "入库", 1000);
        insert("M1", "转库", 1000);
        insert("M1", "出库", 1000);
        insert("M1", "返库", 1000);
        insert("M1", "入库", 5000);
        insert("transfer-only", "转库", 9999);
        insert("out-only", "出库", 9999);
        insert("return-only", "返库", 9999);
        insert("refund-only", "退货", 9999);
        int unoperatedNull = insert("M2", null, 2000);
        int unoperatedEmpty = insert("M3", "", 3000);
        insert("M2", "入库", 4000);

        Map<String, Object> params = Collections.emptyMap();
        assertEquals(Arrays.asList(unoperatedEmpty, unoperatedNull, inbound), ids(params));
        assertEquals(3, service.countArrivalRecords(params));
        assertEquals(6000, service.getArrivalRecords(params).stream()
                .mapToDouble(ShipmentDetails::getWeight).sum(), 0);
        assertEquals(12, service.getAllShipmentDetails(new HashMap<>()).size());
        assertEquals(12, service.countShipmentDetails(params));
        mvc.perform(get("/shipment/arrivals").param("operationType", "转库"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(get("/shipment")).andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(12));
    }

    @Test
    public void deduplicatesGloballyBeforeApplyingDateAndCustomerFilters() throws Exception {
        int first = insert("M1", "入库", 1000);
        int repeated = insert("M1", "入库", 4000, "2026-10-27");
        try (PreparedStatement sql = session.getConnection().prepareStatement(
                "UPDATE shipment_details SET customer = '客户乙' WHERE id = ?")) {
            sql.setInt(1, repeated);
            sql.executeUpdate();
        }
        int other = insert("M2", null, 2000, "2026-10-27");
        mvc.perform(get("/shipment/arrivals").param("startDate", "2026-10-27").param("endDate", "2026-10-27"))
                .andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.data[0].id").value(other));
        mvc.perform(get("/shipment/arrivals").param("customer", "客户乙"))
                .andExpect(jsonPath("$.count").value(0)).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/shipment/arrivals").param("startDate", "2026-10-19").param("endDate", "2026-10-19"))
                .andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.data[0].id").value(first));
    }

    @Test
    public void countAndPagesUseTheSameDeduplicatedRecordsInOriginalIdOrder() throws Exception {
        int a = insert("A", "入库", 100);
        insert("A", "转库", 100);
        int b = insert("B", null, 200);
        insert("B", "出库", 200);
        int c = insert("C", "", 300);
        insert("C", "转库", 300);
        int[] expected = {c, b, a};
        for (int page = 1; page <= expected.length; page++) {
            mvc.perform(get("/shipment/arrivals").param("page", String.valueOf(page)).param("size", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(expected[page - 1]));
        }
        mvc.perform(get("/shipment/arrivals").param("page", "4").param("size", "1"))
                .andExpect(jsonPath("$.count").value(3)).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    public void allSearchFieldsApplyEquallyToListAndCount() throws Exception {
        insert("M1", "入库", 1000);
        insert("M1", "转库", 1000);
        Map<String, String> filters = new HashMap<>();
        filters.put("uniqueIdentifier", "M1");
        filters.put("invoiceNumber", "INV");
        filters.put("customer", "客户甲");
        filters.put("tradeMode", "一般贸易");
        filters.put("steelMill", "江阴材");
        filters.put("steelGrade", "3B");
        filters.put("dimensions", "32.35");
        filters.put("supplierBatchNumber", "B12");
        filters.put("furnaceNumber", "F12");
        filters.put("placementArea", "C10区");
        filters.put("purchaser", "购买方甲");
        filters.put("state", "正常");
        mvc.perform(filtered(filters)).andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data.length()").value(1));
        for (String field : filters.keySet()) {
            Map<String, String> changed = new HashMap<>(filters);
            changed.put(field, "not-a-match");
            mvc.perform(filtered(changed)).andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    private MockHttpServletRequestBuilder filtered(Map<String, String> filters) {
        MockHttpServletRequestBuilder request = get("/shipment/arrivals");
        filters.forEach(request::param);
        return request;
    }

    @Test
    public void preservesUnidentifiedLegacyArrivalsIndividually() throws Exception {
        insert(null, "入库", 10);
        insert(null, null, 20);
        insert("", "", 30);
        insert("", "入库", 40);
        insert(" ", null, 50);
        insert(null, "转库", 60);
        assertEquals(5, service.countArrivalRecords(Collections.emptyMap()));
        assertEquals(150, service.getArrivalRecords(Collections.emptyMap()).stream()
                .mapToDouble(ShipmentDetails::getWeight).sum(), 0);
    }

    @Test
    public void doesNotPromoteLeftoverMovementsOrLaterReentryAfterOriginalIsDeleted() throws Exception {
        int original = insert("M1", "入库", 1000);
        insert("M1", "转库", 1000);
        insert("M1", "出库", 1000);
        insert("M1", "入库", 1000);
        service.deleteShipmentDetailById(original);
        assertEquals(0, service.countArrivalRecords(Collections.emptyMap()));
        assertTrue(service.getArrivalRecords(Collections.emptyMap()).isEmpty());
        assertEquals(3, service.countShipmentDetails(Collections.emptyMap()));
    }

    @Test
    public void arrivalDateRangeIncludesTheWholeDayAndSupportsEmptyPlacementArea() throws Exception {
        insert("before", "入库", 10, "2026-10-18 23:59:59");
        int first = insert("first", null, 20, "2026-10-19 00:00:00");
        int last = insert("last", "入库", 30, "2026-10-19 23:59:59");
        insert("after", "入库", 40, "2026-10-20 00:00:00");
        try (Statement sql = session.getConnection().createStatement()) {
            sql.executeUpdate("UPDATE shipment_details SET placement_area = NULL");
        }
        mvc.perform(get("/shipment/arrivals").param("startDate", "2026-10-19").param("endDate", "2026-10-19")
                        .param("placementArea", "empty"))
                .andExpect(jsonPath("$.count").value(2)).andExpect(jsonPath("$.data[0].id").value(last))
                .andExpect(jsonPath("$.data[1].id").value(first));
    }
}
