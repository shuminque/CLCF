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
            statement.execute("CREATE TABLE shipment_details (id INT PRIMARY KEY, arrival_date DATE, " +
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
