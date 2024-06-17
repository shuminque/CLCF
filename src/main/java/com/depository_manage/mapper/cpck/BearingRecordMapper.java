package com.depository_manage.mapper.cpck;

import com.depository_manage.entity.BearingRecord;
import com.depository_manage.entity.ShipmentDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface BearingRecordMapper {
    // 插入新的记录
    void insertBearingRecord(BearingRecord record);

    // 更新记录
    void updateBearingRecord(BearingRecord record);

    // 根据 ID 删除记录
    void deleteBearingRecordById(int id);

    // 根据 ID 查询记录
    BearingRecord selectBearingRecordById(int id);

    // 获取所有记录
    int countBearingRecords(Map<String, Object> params);
    List<BearingRecord> selectAllBearingRecords(Map<String,Object> params);

    boolean hasSpecialRecord(@Param("boxText") String boxText,
                             @Param("boxNumber") String boxNumber,
                             @Param("depository") String depository,
                             @Param("quantity") int quantity,
                             @Param("iter") int iter);

    List<BearingRecord> selectInventoryByCutoffDate(Map<String, Object> params);

    List<Map<String, Object>> everyPair(
            @Param("depository") String depository,
            @Param("StartDate") String startDate,
            @Param("EndDate") String endDate,
            @Param("state") String state
    );

    List<Map<String, Object>> selectInventoryStatus(
            @Param("date") Date cutoffDate,
            @Param("depository") String depository,
            @Param("state") String state
    );
    int countInOrTransferInRecords(@Param("boxText") String boxText,
                                   @Param("boxNumber") String boxNumber,
                                   @Param("depository") String depository,
                                   @Param("iter") int iter);
    int calculateNetInOrTransferInVsOut(@Param("boxText") String boxText,
                                        @Param("boxNumber") String boxNumber,
                                        @Param("depository") String depository,
                                        @Param("iter") int iter);
    boolean findTransferInRecord(@Param("boxText") String boxText,
                                 @Param("boxNumber") String boxNumber,
                                 @Param("iter") int iter);
    List<Map<String, Object>> selectComprehensiveTransferRecords(Map<String, Object> params);
    int selectComprehensiveTransferRecordsCount();
    Map<String, Object> selectCountsByDateAndDepository(@Param("date") String date,
                                                        @Param("depository") String depository);

    boolean findTransferOutRecord(@Param("boxText") String boxText, @Param("boxNumber") String boxNumber, @Param("iter") int iter);

    // 查询客户、型号和在库数量
    Integer getDistinctCustomerModelAndInStockQuantity(@Param("customer") String customer,
                                                   @Param("outerInnerRing") String outerInnerRing,
                                                   @Param("model") String model,
                                                   @Param("time") String time);
    List<Map<String, String>> getAllWEs(Map<String, Object> params);

    List<BearingRecord> getOutPDs(Map<String, Object> params);

}
