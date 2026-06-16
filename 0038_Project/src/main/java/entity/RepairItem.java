package entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private transient boolean historyEnabled = false;

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

    public void enableHistory() {
        this.historyEnabled = true;
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
        String old = this.itemName;
        this.itemName = itemName;
        if ((old == null && itemName != null) || (old != null && !old.equals(itemName))) {
            addHistory("ITEM_NAME_CHANGED",
                    old == null ? "" : old,
                    itemName == null ? "" : itemName,
                    null);
        }
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        ItemType old = this.itemType;
        this.itemType = itemType;
        if (old != itemType) {
            addHistory("ITEM_TYPE_CHANGED",
                    old == null ? "" : old.getDisplayName(),
                    itemType == null ? "" : itemType.getDisplayName(),
                    null);
        }
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        String old = this.problemDescription;
        this.problemDescription = problemDescription;
        if ((old == null && problemDescription != null) || (old != null && !old.equals(problemDescription))) {
            addHistory("DESCRIPTION_CHANGED",
                    old == null ? "" : old,
                    problemDescription == null ? "" : problemDescription,
                    null);
        }
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
        BigDecimal old = this.cost;
        this.cost = cost;
        if ((old == null && cost != null)
                || (old != null && cost != null && old.compareTo(cost) != 0)
                || (old != null && cost == null)) {
            addHistory("COST_CHANGED",
                    old == null ? "0" : old.toPlainString(),
                    cost == null ? "0" : cost.toPlainString(),
                    null);
        }
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
        if (!historyEnabled) {
            return;
        }
        getHistory().add(new RepairHistoryEntry(action, oldValue, newValue, remark));
    }

    public void markCreated() {
        if (historyEnabled) {
            addHistory("CREATED", null, null, "记录已创建");
        }
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
