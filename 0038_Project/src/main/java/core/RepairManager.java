package core;

import entity.RepairItem;
import entity.RepairStatus;
import file.RepairFileRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public RepairItem addItem(RepairItem item) {
        items.add(item);
        saveToFile();
        return item;
    }

    public List<RepairItem> getAllItems() {
        return items.stream()
                .sorted(Comparator.comparing(RepairItem::getReportDate).reversed())
                .collect(Collectors.toList());
    }

    public List<RepairItem> getItemsByStatus(RepairStatus status) {
        return items.stream()
                .filter(item -> item.getStatus() == status)
                .sorted(Comparator.comparing(RepairItem::getReportDate).reversed())
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
            itemOpt.get().setStatus(status);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean updateRemark(String id, String remark) {
        Optional<RepairItem> itemOpt = getItemById(id);
        if (itemOpt.isPresent()) {
            itemOpt.get().setRemark(remark);
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
}
