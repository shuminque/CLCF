package com.depository_manage.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
@Data
public class ShipmentDetails {
    private Integer id;
    private String invoiceNumber;
    private String customer;
    private String tradeMode;
    private String deliveryPoint;
    private Date arrivalPortDate;
    private Date arrivalDate;
    private String dimensions;
    private Double weight;
    private String steelMill;
    private String furnaceNumber;
    private String invoiceApplication;
    private String operationType;
    private String supplierBatchNumber;
    private Integer bundleCount;
    private String placementArea;
    // Getters and Setters
}