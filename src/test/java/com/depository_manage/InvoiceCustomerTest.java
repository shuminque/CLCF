package com.depository_manage;

import com.depository_manage.controller.clck.ShipmentDetailsController;
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
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class InvoiceCustomerTest {
    private SqlSession session;
    private ShipmentDetailsService service;
    private MockMvc mvc;

    @Before
    public void setup() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        Configuration configuration = new Configuration(new Environment("test",
                new JdbcTransactionFactory(), dataSource));
        String resource = "mapper/clck/ShipmentDetailsMapper.xml";
        try (InputStream stream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE shipment_details (id INT PRIMARY KEY, arrival_date TIMESTAMP, " +
                    "steel_mill VARCHAR(100), steel_grade VARCHAR(100), dimensions VARCHAR(100), " +
                    "trade_mode VARCHAR(100), customer VARCHAR(100), weight DECIMAL(12, 2), " +
                    "invoice_application VARCHAR(100), unit_price DECIMAL(12, 5), operation_type VARCHAR(100))");
        }
        insert(1, "客户甲", 10, "入库");
        insert(2, "客户甲", 20, "入库");
        insert(3, "客户乙", 40, "入库");
        insert(4, null, 5, "入库");
        insert(5, "", 4, "入库");
        service = new ShipmentDetailsServiceImpl(session.getMapper(ShipmentDetailsMapper.class));
        ShipmentDetailsController controller = new ShipmentDetailsController();
        ReflectionTestUtils.setField(controller, "shipmentDetailsService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @After
    public void close() {
        if (session != null) session.close();
    }

    private void insert(int id, String customer, int weight, String operation) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO shipment_details VALUES (?, '2026-10-23', '江阴材', 'SUJ2S1', '35.40', " +
                        "'一般贸易', ?, ?, '', 6.50863, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, customer);
            statement.setInt(3, weight);
            statement.setString(4, operation);
            statement.executeUpdate();
        }
    }

    private MockHttpServletRequestBuilder group(MockHttpServletRequestBuilder request) {
        return request.param("arrival_date", "2026-10-23").param("steel_mill", "江阴材")
                .param("steel_grade", "SUJ2S1").param("dimensions", "35.40").param("trade_mode", "一般贸易");
    }

    private void assertInvoice(int id, String status, String price) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT invoice_application, unit_price FROM shipment_details WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(status, result.getString(1));
                assertEquals(0, new BigDecimal(price).compareTo(result.getBigDecimal(2)));
            }
        }
    }

    @Test
    public void groupsInboundWeightsByCustomerAndReturnsCustomerToTheTable() throws Exception {
        insert(6, "客户甲", 100, "出库");
        List<Map<String, Object>> rows = service.queryShipmentDetails(Collections.emptyMap());
        assertEquals(3, rows.size());
        for (Map<String, Object> row : rows) {
            String customer = (String) row.get("customer");
            assertNotNull(customer);
            int expectedWeight = "客户甲".equals(customer) ? 30 : "客户乙".equals(customer) ? 40 : 9;
            assertEquals(expectedWeight, ((Number) row.get("weight")).intValue());
        }
        mvc.perform(get("/shipment/query"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.data[*].customer").value(
                        org.hamcrest.Matchers.containsInAnyOrder("", "客户甲", "客户乙")));
    }

    @Test
    public void customerSearchFiltersExactlyAndKeepsArrivalDateOrder() throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("UPDATE shipment_details SET arrival_date = '2026-10-24' WHERE id = 1");
            statement.executeUpdate("UPDATE shipment_details SET arrival_date = '2026-10-22' WHERE id = 2");
        }
        mvc.perform(get("/shipment/query").param("customer", "客户甲")
                        .param("steelMill", "江阴材").param("steelGrade", "SUJ2S1")
                        .param("dimensions", "35.40").param("startDate", "2026-10-22")
                        .param("endDate", "2026-10-24"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.data[0].customer").value("客户甲"))
                .andExpect(jsonPath("$.data[1].customer").value("客户甲"))
                .andExpect(jsonPath("$.data[0].weight").value(20))
                .andExpect(jsonPath("$.data[1].weight").value(10));
        mvc.perform(get("/shipment/query").param("customer", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(4));
        mvc.perform(get("/shipment/query").param("customer", "客户"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/shipment/query").param("customer", "客户甲' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    public void mergeCheckboxCombinesCustomersWithinTheCurrentFilter() throws Exception {
        insert(6, "客户甲", 100, "出库");
        mvc.perform(get("/shipment/query").param("mergeCustomers", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].weight").value(79))
                .andExpect(jsonPath("$.data[0].customer").value("多个客户"))
                .andExpect(jsonPath("$.data[0].merge_customers").value(true))
                .andExpect(jsonPath("$.data[0].customer_filter").value(""));
        mvc.perform(get("/shipment/query").param("mergeCustomers", "true").param("customer", "客户甲"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].weight").value(30))
                .andExpect(jsonPath("$.data[0].customer_filter").value("客户甲"));
        mvc.perform(get("/shipment/query").param("mergeCustomers", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.data[0].merge_customers").value(false));
    }

    @Test
    public void mergedModeKeepsOriginalGroupingFieldsAndDateOrder() throws Exception {
        String[][] variations = {{"arrival_date", "2026-10-22"}, {"steel_mill", "另一个钢厂"},
                {"steel_grade", "另一钢等"}, {"dimensions", "36.40"}, {"trade_mode", "来料加工"},
                {"invoice_application", "已结算"}, {"unit_price", "7.12345"}};
        for (int i = 0; i < variations.length; i++) {
            insert(6 + i, "客户乙", 10, "入库");
            try (PreparedStatement statement = session.getConnection().prepareStatement(
                    "UPDATE shipment_details SET " + variations[i][0] + " = ? WHERE id = ?")) {
                statement.setString(1, variations[i][1]);
                statement.setInt(2, 6 + i);
                statement.executeUpdate();
            }
        }
        mvc.perform(get("/shipment/query").param("mergeCustomers", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(8))
                .andExpect(jsonPath("$.data[0].weight").value(10));
    }

    @Test
    public void mergedDetailsAndEditsIncludeAllCustomersButOnlyTheSelectedGroup() throws Exception {
        insert(6, "客户乙", 1, "入库");
        insert(7, "客户乙", 1, "入库");
        insert(8, "客户乙", 1, "入库");
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("UPDATE shipment_details SET invoice_application = '已结算' WHERE id = 6");
            statement.executeUpdate("UPDATE shipment_details SET unit_price = 9 WHERE id = 7");
            statement.executeUpdate("UPDATE shipment_details SET invoice_application = NULL WHERE id = 8");
        }
        mvc.perform(group(get("/shipment/selectInvoiceApplication"))
                        .param("mergeCustomers", "true").param("unit_price", "6.50863")
                        .param("original_invoice_application", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(group(post("/shipment/updateInvoice"))
                        .param("mergeCustomers", "true").param("original_invoice_application", "")
                        .param("invoice_application", "已核对开票、未结算")
                        .param("original_unit_price", "6.50863").param("new_unit_price", "7.12345"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        for (int id = 1; id <= 5; id++) assertInvoice(id, "已核对开票、未结算", "7.12345");
        assertInvoice(6, "已结算", "6.50863");
        assertInvoice(7, "", "9");
        assertInvoice(8, null, "6.50863");
    }

    @Test
    public void mergedOperationsRespectTheCustomerSearchFilter() throws Exception {
        mvc.perform(group(get("/shipment/selectInvoiceApplication"))
                        .param("mergeCustomers", "true").param("customer", "客户甲")
                        .param("original_invoice_application", "").param("unit_price", "6.50863"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(group(post("/shipment/updateInvoice"))
                        .param("mergeCustomers", "true").param("customer", "客户甲")
                        .param("original_invoice_application", "").param("invoice_application", "已结算")
                        .param("original_unit_price", "6.50863").param("new_unit_price", "7.12345"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertInvoice(1, "已结算", "7.12345");
        assertInvoice(2, "已结算", "7.12345");
        for (int id = 3; id <= 5; id++) assertInvoice(id, "", "6.50863");
    }

    @Test
    public void mergedNullStatusDoesNotIncludeEmptyStatusRecords() throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("UPDATE shipment_details SET invoice_application = NULL WHERE id IN (1, 3)");
        }
        mvc.perform(group(get("/shipment/selectInvoiceApplication"))
                        .param("mergeCustomers", "true").param("unit_price", "6.50863"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(group(post("/shipment/updateInvoice"))
                        .param("mergeCustomers", "true").param("invoice_application", "已结算")
                        .param("original_unit_price", "6.50863").param("new_unit_price", "7.12345"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertInvoice(1, "已结算", "7.12345");
        assertInvoice(3, "已结算", "7.12345");
        assertInvoice(2, "", "6.50863");
        assertInvoice(4, "", "6.50863");
        assertInvoice(5, "", "6.50863");
    }

    @Test
    public void detailsAndInvoiceEditsStayWithinTheSelectedCustomer() throws Exception {
        mvc.perform(group(get("/shipment/selectInvoiceApplication"))
                        .param("customer", "客户甲").param("unit_price", "6.50863"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customer").value("客户甲"))
                .andExpect(jsonPath("$[1].customer").value("客户甲"));
        mvc.perform(group(post("/shipment/updateInvoice"))
                        .param("customer", "客户甲").param("invoice_application", "已结算")
                        .param("original_unit_price", "6.50863").param("new_unit_price", "7.12345"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertInvoice(1, "已结算", "7.12345");
        assertInvoice(2, "已结算", "7.12345");
        assertInvoice(3, "", "6.50863");
        assertInvoice(4, "", "6.50863");
        assertInvoice(5, "", "6.50863");
    }

    @Test
    public void blankCustomerIncludesLegacyNullsWithoutEditingNamedCustomers() throws Exception {
        mvc.perform(group(get("/shipment/selectInvoiceApplication")).param("unit_price", "6.50863"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(group(post("/shipment/updateInvoice"))
                        .param("customer", "").param("invoice_application", "已核对开票、未结算")
                        .param("original_unit_price", "6.50863").param("new_unit_price", "8.12345"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertInvoice(4, "已核对开票、未结算", "8.12345");
        assertInvoice(5, "已核对开票、未结算", "8.12345");
        assertInvoice(1, "", "6.50863");
        assertInvoice(2, "", "6.50863");
        assertInvoice(3, "", "6.50863");
    }
}
