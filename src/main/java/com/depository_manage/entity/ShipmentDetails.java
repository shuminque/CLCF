package com.depository_manage.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
@Data
public class ShipmentDetails {
    private Integer id;
    private String uniqueIdentifier;  // 新增的字段
    private String invoiceNumber;
    private String customer;
    private String tradeMode;
    private String deliveryPoint;
    private Date arrivalPortDate;
    private Date arrivalDate;
    private String steelGrade;
    private String dimensions;
    private Double weight;
    private String steelMill;
    private String furnaceNumber;
    private String invoiceApplication;
    private String operationType;
    private String supplierBatchNumber;
    private Integer bundleCount;
    private String placementArea;
    private String purchaser;
    private Date time;
    private BigDecimal unitPrice; // 新增的字段
    private String state;

    // Getters and Setters
}