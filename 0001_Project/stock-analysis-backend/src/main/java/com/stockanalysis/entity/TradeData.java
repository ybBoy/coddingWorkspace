package com.stockanalysis.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class TradeData {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    @ExcelProperty(index = 0)
    private String tradeTimeStr;

    @ExcelProperty(index = 1)
    private Double tradePrice;

    @ExcelProperty(index = 2)
    private Integer tradeQuantity;

    @ExcelProperty(index = 3)
    private String buyAccount;

    @ExcelProperty(index = 4)
    private String sellAccount;

    @ExcelIgnore
    private LocalDateTime tradeTime;

    public LocalDateTime getTradeTime() {
        if (tradeTime == null && tradeTimeStr != null) {
            try {
                tradeTime = LocalDateTime.parse(tradeTimeStr.trim(), FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
        return tradeTime;
    }
}