package com.depository_manage.mapper.clck;

import com.depository_manage.entity.ShipmentDetails;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShipmentDetailsMapper {

    // Existing methods
    int insertShipmentDetail(ShipmentDetails shipmentDetails);
    ShipmentDetails selectShipmentDetailById(@Param("id") int id);
    List<ShipmentDetails> selectAllShipmentDetails();
    int updateShipmentDetail(ShipmentDetails shipmentDetails);
    int deleteShipmentDetail(@Param("id") int id);

    // New method for batch insertion
    int insertShipmentDetails(List<ShipmentDetails> shipmentDetailsList);
}
