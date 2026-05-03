package com.warehouse.dto;

public class StockOperationDto {
    private String partId;
    private int quantity;

    public StockOperationDto() {
    }

    public StockOperationDto(String partId, int quantity) {
        this.partId = partId;
        this.quantity = quantity;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
