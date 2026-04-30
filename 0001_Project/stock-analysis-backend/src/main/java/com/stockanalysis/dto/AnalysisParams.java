package com.stockanalysis.dto;

import lombok.Data;

@Data
public class AnalysisParams {
    private Integer pricePercentile = 30;
    private Integer timePercentile = 20;
    private Double sellPriceMultiple = 3.0;
}