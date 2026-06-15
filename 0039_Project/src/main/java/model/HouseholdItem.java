package model;

import java.math.BigDecimal;
import java.util.UUID;

public class HouseholdItem {
    private String id;
    private String name;
    private String category;
    private DisposePlan disposePlan;
    private ItemStatus status;
    private BigDecimal estimatedPrice;
    private String location;
    private String imageUrl;
    private String remark;
    private long createdAt;
    private long updatedAt;

    public HouseholdItem() {
        this.id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public HouseholdItem(String name, String category, DisposePlan disposePlan,
                         BigDecimal estimatedPrice, String location, String remark) {
        this();
        this.name = name;
        this.category = category;
        this.disposePlan = disposePlan;
        this.status = ItemStatus.getDefaultForPlan(disposePlan);
        this.estimatedPrice = estimatedPrice;
        this.location = location;
        this.remark = remark;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public DisposePlan getDisposePlan() {
        return disposePlan;
    }

    public void setDisposePlan(DisposePlan disposePlan) {
        this.disposePlan = disposePlan;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}
