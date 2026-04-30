package com.stockanalysis.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PositionResult {
    private String trader;
    private LocalDateTime positionTime;
    private Double positionPrice;
    private Integer positionQuantity;
    private String buyAccount;
}
