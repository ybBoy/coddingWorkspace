package com.stockanalysis.dto;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisResult {
    private List<PositionResult> positionResults;
    private List<SellResult> sellResults;
    private AnalysisParams params;
}
