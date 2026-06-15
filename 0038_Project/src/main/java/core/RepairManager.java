package core;

import entity.ItemType;
import entity.RepairItem;
import entity.RepairStatus;
import file.RepairFileRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RepairManager {
    private List<RepairItem> items;
    private final RepairFileRepository fileRepository;

    public RepairManager() {
        this.fileRepository = new RepairFileRepository();
        this.items = new ArrayList<>();
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            List<RepairItem> loaded = fileRepository.loadAll();
            if (loaded != null) {
                this.items = loaded;
                for (RepairItem item : this.items) {
                    if (item.getItemType() == null) {
                        item.setItemType(ItemType.OTHER);
                    }
                    if (item.getImages() == null) {
                        item.setImages(new ArrayList<>());
                    }
                    if (item.getHistory() == null) {
                        item.setHistory(new ArrayList<>());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load data from file: " + e.getMessage());
            this.items = new ArrayList<>();
        }
    }

    private void saveToFile() {
        try {
            fileRepository.saveAll(this.items);
        } catch (IOException e) {
            System.err.println("Failed to save data to file: " + e.getMessage());
        }
    }

    public void saveAll() {
        saveToFile();
    }

    public RepairItem addItem(RepairItem item) {
        item.markCreated();
        items.add(item);
        saveToFile();
        return item;
    }

    private Comparator<RepairItem> getSmartSort() {
        return (a, b) -> {
            boolean aOver = a.isOverdue();
            boolean bOver = b.isOverdue();
            if (aOver != bOver) {
                return aOver ? -1 : 1;
            }
            boolean aPend = a.getStatus() == RepairStatus.PENDING;
            boolean bPend = b.getStatus() == RepairStatus.PENDING;
            if (aPend != bPend) {
                return aPend ? -1 : 1;
            }
            return b.getReportDate().compareTo(a.getReportDate());
        };
    }

    public List<RepairItem> getAllItems() {
        return items.stream()
                .sorted(getSmartSort())
                .collect(Collectors.toList());
    }

    public List<RepairItem> getItemsByStatus(RepairStatus status) {
        return items.stream()
                .filter(item -> item.getStatus() == status)
                .sorted(getSmartSort())
                .collect(Collectors.toList());
    }

    public List<RepairItem> getItemsByType(ItemType type) {
        return items.stream()
                .filter(item -> item.getItemType() == type)
                .sorted(getSmartSort())
                .collect(Collectors.toList());
    }

    public List<RepairItem> searchItems(String keyword, RepairStatus status, ItemType type) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return items.stream()
                .filter(item -> {
                    if (status != null && item.getStatus() != status) return false;
                    if (type != null && item.getItemType() != type) return false;
                    if (!kw.isEmpty()) {
                        boolean hit = false;
                        if (item.getItemName() != null && item.getItemName().toLowerCase().contains(kw)) hit = true;
                        if (item.getProblemDescription() != null && item.getProblemDescription().toLowerCase().contains(kw)) hit = true;
                        if (item.getRemark() != null && item.getRemark().toLowerCase().contains(kw)) hit = true;
                        return hit;
                    }
                    return true;
                })
                .sorted(getSmartSort())
                .collect(Collectors.toList());
    }

    public Optional<RepairItem> getItemById(String id) {
        return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    public boolean updateStatus(String id, RepairStatus status) {
        Optional<RepairItem> itemOpt = getItemById(id);
        if (itemOpt.isPresent()) {
            RepairItem item = itemOpt.get();
            item.setStatus(status);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean updateRemark(String id, String remark) {
        Optional<RepairItem> itemOpt = getItemById(id);
        if (itemOpt.isPresent()) {
            RepairItem item = itemOpt.get();
            item.setRemark(remark);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean addImageToItem(String itemId, entity.RepairImage image) {
        Optional<RepairItem> itemOpt = getItemById(itemId);
        if (itemOpt.isPresent()) {
            itemOpt.get().addImage(image);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean removeImageFromItem(String itemId, String imageId) {
        Optional<RepairItem> itemOpt = getItemById(itemId);
        if (itemOpt.isPresent()) {
            itemOpt.get().removeImage(imageId);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean deleteItem(String id) {
        boolean removed = items.removeIf(item -> item.getId().equals(id));
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    public BigDecimal getTotalCost() {
        return items.stream()
                .map(RepairItem::getCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getPendingCount() {
        return items.stream()
                .filter(item -> item.getStatus() == RepairStatus.PENDING)
                .count();
    }

    public Map<String, BigDecimal> getCostByMonth(int months) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            result.put(ym.toString(), BigDecimal.ZERO);
        }
        for (RepairItem item : items) {
            if (item.getReportDate() == null || item.getCost() == null) continue;
            YearMonth ym = YearMonth.from(item.getReportDate());
            String key = ym.toString();
            if (result.containsKey(key)) {
                result.put(key, result.get(key).add(item.getCost()));
            }
        }
        return result;
    }

    public Map<String, BigDecimal> getCostByType() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (ItemType type : ItemType.values()) {
            result.put(type.getDisplayName(), BigDecimal.ZERO);
        }
        for (RepairItem item : items) {
            if (item.getCost() == null) continue;
            ItemType type = item.getItemType() == null ? ItemType.OTHER : item.getItemType();
            String key = type.getDisplayName();
            result.put(key, result.getOrDefault(key, BigDecimal.ZERO).add(item.getCost()));
        }
        return result;
    }

    public Map<String, BigDecimal> getCostByStatus() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (RepairStatus status : RepairStatus.values()) {
            result.put(status.getDisplayName(), BigDecimal.ZERO);
        }
        for (RepairItem item : items) {
            if (item.getCost() == null) continue;
            String key = item.getStatus().getDisplayName();
            result.put(key, result.getOrDefault(key, BigDecimal.ZERO).add(item.getCost()));
        }
        return result;
    }

    public Map<String, Object> getAllStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", getTotalCost());
        stats.put("pendingCount", getPendingCount());
        stats.put("totalCount", items.size());
        stats.put("overdueCount", items.stream().filter(RepairItem::isOverdue).count());
        stats.put("costByMonth", getCostByMonth(6));
        stats.put("costByType", getCostByType());
        stats.put("costByStatus", getCostByStatus());
        return stats;
    }

    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,物品名称,物品类型,问题描述,报修日期,状态,费用,备注,逾期天数\n");
        for (RepairItem item : getAllItems()) {
            sb.append(escapeCsv(item.getId())).append(",");
            sb.append(escapeCsv(item.getItemName())).append(",");
            sb.append(escapeCsv(item.getItemType() == null ? "" : item.getItemType().getDisplayName())).append(",");
            sb.append(escapeCsv(item.getProblemDescription())).append(",");
            sb.append(item.getReportDate()).append(",");
            sb.append(escapeCsv(item.getStatus() == null ? "" : item.getStatus().getDisplayName())).append(",");
            sb.append(item.getCost() == null ? "0" : item.getCost().toPlainString()).append(",");
            sb.append(escapeCsv(item.getRemark())).append(",");
            sb.append(item.getOverdueDays()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
