package com.depository_manage.mapper.cpck;

import com.depository_manage.entity.Bearing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BearingMapper {
    Bearing selectBearingByBoxTextAndDepository(@Param("boxText") String boxText,
                                                @Param("depository") String depository);
    List<Bearing> selectAllBearings(Map<String,Object> params);
    void updateBearing(Bearing bearing);
    Bearing selectLatestBearingByBoxText(String boxText);
    void insertBearing(Bearing bearing);

    Integer calculateTotalQuantityByBoxText(@Param("boxText") String boxText,
                                            @Param("depository") String depository);
    List<String> searchBoxText(@Param("query") String query, @Param("depository") String depository);

    void deleteBearingById(Integer id);
    String getMaxPairNumber();
    List<Map<String, String>> getDistinctCustomerAndModel(Map<String, Object> params);

    List<Map<String, String>> getModelInfo(Map<String, Object> params);
}
