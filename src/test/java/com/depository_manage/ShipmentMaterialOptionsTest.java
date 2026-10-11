package com.depository_manage;

import com.depository_manage.controller.clck.ShipmentDetailsController;
import com.depository_manage.service.clck.ShipmentDetailsService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ShipmentMaterialOptionsTest {
    @Test
    public void endpointReturnsMaterialPairsForSelectedMill() throws Exception {
        ShipmentDetailsService service = mock(ShipmentDetailsService.class);
        Map<String, String> pair = new HashMap<>();
        pair.put("steelGrade", "G1");
        pair.put("dimensions", "S1");
        when(service.getMaterialOptions("Mill A")).thenReturn(Collections.singletonList(pair));
        ShipmentDetailsController controller = new ShipmentDetailsController();
        ReflectionTestUtils.setField(controller, "shipmentDetailsService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/shipment/material-options").param("steelMill", "Mill A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].steelGrade").value("G1"))
                .andExpect(jsonPath("$[0].dimensions").value("S1"));
        verify(service).getMaterialOptions("Mill A");
        mvc.perform(get("/shipment/material-options")).andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }

    @Test
    public void mapperUsesBoundMillParameterWithoutPagination() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/clck/ShipmentDetailsMapper.xml";
        try (InputStream stream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("steelMill", "Mill ' A");
        BoundSql boundSql = configuration.getMappedStatement(
                "com.depository_manage.mapper.clck.ShipmentDetailsMapper.selectMaterialOptions").getBoundSql(params);
        String sql = boundSql.getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("SELECT DISTINCT steel_grade AS steelGrade, dimensions"));
        assertTrue(sql.contains("WHERE steel_mill = ?"));
        assertFalse(sql.toUpperCase().contains("LIMIT"));
        assertEquals(1, boundSql.getParameterMappings().size());
        assertEquals("steelMill", boundSql.getParameterMappings().get(0).getProperty());
        assertFalse(sql.contains("Mill ' A"));
    }
}
