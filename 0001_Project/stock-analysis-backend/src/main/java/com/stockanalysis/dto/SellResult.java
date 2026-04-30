package com.stockanalysis.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellResult {
    private String seller;
    private LocalDateTime sellTime;
    private Double sellPrice;
    private Integer sellQuantity;
    private String sellAccount;
    private Double positionPrice;
    private Double profit;
    private Double profitRate;
}
