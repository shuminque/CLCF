package com.depository_manage.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArrivalNoticeRow {
    private String arrivalDate;
    private String deliveryPoint;
    private String tradeMode;
    private String customer;
    private String dimensions;
    private String steelGrade;
    private String steelMill;
    private String furnaceNumber;
    private Long bundleCount;
    private BigDecimal weight;
}
