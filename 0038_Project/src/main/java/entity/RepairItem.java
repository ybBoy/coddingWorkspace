package entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RepairItem {
    private String id;
    private String itemName;
    private ItemType itemType;
    private String problemDescription;
    private LocalDate reportDate;
    private RepairStatus status;
    private BigDecimal cost;
    private String remark;
    private List<RepairImage> images;
    private List<RepairHistoryEntry> history;

    public RepairItem() {
        this.id = UUID.randomUUID().toString();
        this.reportDate = LocalDate.now();
        this.status = RepairStatus.PENDING;
        this.cost = BigDecimal.ZERO;
        this.itemType = ItemType.OTHER;
        this.images = new ArrayList<>();
        this.history = new ArrayList<>();
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

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
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
        RepairStatus old = this.status;
        this.status = status;
        if (old != status) {
            addHistory("STATUS_CHANGED",
                    old == null ? "" : old.getDisplayName(),
                    status == null ? "" : status.getDisplayName(),
                    null);
        }
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
        String old = this.remark;
        this.remark = remark;
        if ((old == null && remark != null) || (old != null && !old.equals(remark))) {
            addHistory("REMARK_UPDATED", old == null ? "" : old, remark == null ? "" : remark, null);
        }
    }

    public List<RepairImage> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    public void setImages(List<RepairImage> images) {
        this.images = images;
    }

    public void addImage(RepairImage image) {
        getImages().add(image);
        addHistory("IMAGE_ADDED", null, image.getFileName(), image.getDescription());
    }

    public void removeImage(String imageId) {
        boolean removed = getImages().removeIf(img -> imageId.equals(img.getId()));
        if (removed) {
            addHistory("IMAGE_REMOVED", imageId, null, null);
        }
    }

    public List<RepairHistoryEntry> getHistory() {
        if (history == null) {
            history = new ArrayList<>();
        }
        return history;
    }

    public void setHistory(List<RepairHistoryEntry> history) {
        this.history = history;
    }

    public void addHistory(String action, String oldValue, String newValue, String remark) {
        getHistory().add(new RepairHistoryEntry(action, oldValue, newValue, remark));
    }

    public void markCreated() {
        addHistory("CREATED", null, null, "记录已创建");
    }

    public boolean isOverdue() {
        if (this.status != RepairStatus.PENDING) {
            return false;
        }
        return LocalDate.now().minusDays(7).isAfter(this.reportDate);
    }

    public long getOverdueDays() {
        if (this.status != RepairStatus.PENDING) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(this.reportDate, LocalDate.now());
        return Math.max(0, days - 7);
    }

    public long getPendingDays() {
        return Math.max(0, ChronoUnit.DAYS.between(this.reportDate, LocalDate.now()));
    }
}
