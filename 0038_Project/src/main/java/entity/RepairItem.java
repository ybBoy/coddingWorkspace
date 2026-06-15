package entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class RepairItem {
    private String id;
    private String itemName;
    private String problemDescription;
    private LocalDate reportDate;
    private RepairStatus status;
    private BigDecimal cost;
    private String remark;

    public RepairItem() {
        this.id = UUID.randomUUID().toString();
        this.reportDate = LocalDate.now();
        this.status = RepairStatus.PENDING;
        this.cost = BigDecimal.ZERO;
    }

    public RepairItem(String itemName, String problemDescription, BigDecimal cost, String remark) {
        this();
        this.itemName = itemName;
        this.problemDescription = problemDescription;
        this.cost = cost != null ? cost : BigDecimal.ZERO;
        this.remark = remark;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isOverdue() {
        if (this.status != RepairStatus.PENDING) {
            return false;
        }
        return LocalDate.now().minusDays(7).isAfter(this.reportDate);
    }
}
