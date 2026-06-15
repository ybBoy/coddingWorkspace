package com.demo.service;

import com.demo.dto.EditBeanRequest;
import com.demo.dto.StatisticsResponse;
import com.demo.dto.StockOperationRequest;
import com.demo.dto.WeeklyConsumeBean;
import com.demo.model.CoffeeBean;
import com.demo.model.RoastLevel;
import com.demo.model.StockRecord;
import com.demo.repository.FileBeanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BeanInventoryService {

    private final FileBeanRepository repository;

    public BeanInventoryService(FileBeanRepository repository) {
        this.repository = repository;
    }

    public List<CoffeeBean> getAllBeans() {
        return repository.findAll();
    }

    public List<CoffeeBean> searchAndSort(String search, String roastLevel,
                                           String sortBy, String sortDir) {
        List<CoffeeBean> beans = repository.findAll();
        String keyword = (search != null) ? search.trim().toLowerCase() : "";
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "name";
        String direction = (sortDir != null) ? sortDir.toLowerCase() : "asc";

        beans = beans.stream()
                .filter(bean -> {
                    if (roastLevel != null && !roastLevel.isEmpty() && !bean.getRoastLevel().equals(roastLevel)) {
                        return false;
                    }
                    if (!keyword.isEmpty()) {
                        String name = (bean.getName() != null) ? bean.getName().toLowerCase() : "";
                        String origin = (bean.getOrigin() != null) ? bean.getOrigin().toLowerCase() : "";
                        return name.contains(keyword) || origin.contains(keyword);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Comparator<CoffeeBean> comparator;
        switch (sortField) {
            case "stock":
                comparator = Comparator.comparingInt(CoffeeBean::getStockGrams);
                break;
            case "minStock":
                comparator = Comparator.comparingInt(CoffeeBean::getMinStockLevel);
                break;
            case "lastModified":
                comparator = Comparator.comparing(CoffeeBean::getLastRecordTime);
                break;
            case "origin":
                comparator = Comparator.comparing(b -> (b.getOrigin() != null ? b.getOrigin() : ""),
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(b -> (b.getName() != null ? b.getName() : ""),
                        String.CASE_INSENSITIVE_ORDER);
                break;
        }

        if ("desc".equals(direction)) {
            comparator = comparator.reversed();
        }

        beans.sort(comparator);
        return beans;
    }

    public Optional<CoffeeBean> getBeanById(String id) {
        return repository.findById(id);
    }

    public CoffeeBean addBean(CoffeeBean bean) {
        return addBeanWithOperator(bean, null);
    }

    public CoffeeBean addBeanWithOperator(CoffeeBean bean, String operator) {
        if (bean.getStockGrams() < 0) {
            throw new IllegalArgumentException("Stock grams cannot be negative");
        }
        if (bean.getMinStockLevel() < 0) {
            throw new IllegalArgumentException("Min stock level cannot be negative");
        }
        if (bean.getName() == null || bean.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Bean name is required");
        }
        if (!RoastLevel.isValid(bean.getRoastLevel())) {
            throw new IllegalArgumentException("Invalid roast level: " + bean.getRoastLevel());
        }
        String id = UUID.randomUUID().toString();
        bean.setId(id);
        LocalDateTime now = LocalDateTime.now();
        bean.setCreatedAt(now);
        bean.setLastModified(now);
        if (bean.getStockRecords() == null) {
            bean.setStockRecords(new ArrayList<>());
        }
        if (bean.getStockGrams() > 0) {
            StockRecord record = createRecord(id, "INIT",
                    0, bean.getStockGrams(), bean.getStockGrams(),
                    operator != null ? operator : "system", "Initial stock setup");
            bean.getStockRecords().add(record);
        }
        return repository.save(bean);
    }

    public CoffeeBean updateBean(String id, EditBeanRequest req) {
        Optional<CoffeeBean> beanOpt = repository.findById(id);
        if (!beanOpt.isPresent()) {
            throw new IllegalArgumentException("Bean not found: " + id);
        }
        CoffeeBean bean = beanOpt.get();
        List<String> changes = new ArrayList<>();
        int stockBefore = bean.getStockGrams();

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            if (!newName.equals(bean.getName())) {
                changes.add("name: " + bean.getName() + " -> " + newName);
                bean.setName(newName);
            }
        }
        if (req.getOrigin() != null) {
            String newOrigin = req.getOrigin().trim();
            String oldOrigin = bean.getOrigin() == null ? "" : bean.getOrigin();
            if (!newOrigin.equals(oldOrigin)) {
                changes.add("origin: " + oldOrigin + " -> " + newOrigin);
                bean.setOrigin(newOrigin);
            }
        }
        if (req.getRoastLevel() != null && !req.getRoastLevel().isEmpty()) {
            if (!RoastLevel.isValid(req.getRoastLevel())) {
                throw new IllegalArgumentException("Invalid roast level: " + req.getRoastLevel());
            }
            if (!req.getRoastLevel().equals(bean.getRoastLevel())) {
                changes.add("roastLevel: " + bean.getRoastLevel() + " -> " + req.getRoastLevel());
                bean.setRoastLevel(req.getRoastLevel());
            }
        }
        if (req.getMinStockLevel() != null) {
            if (req.getMinStockLevel() < 0) {
                throw new IllegalArgumentException("Min stock level cannot be negative");
            }
            if (!req.getMinStockLevel().equals(bean.getMinStockLevel())) {
                changes.add("minStockLevel: " + bean.getMinStockLevel() + " -> " + req.getMinStockLevel());
                bean.setMinStockLevel(req.getMinStockLevel());
            }
        }

        if (!changes.isEmpty()) {
            bean.setLastModified(LocalDateTime.now());
            String remark = "Edit info - " + String.join("; ", changes);
            String operator = (req.getOperator() != null && !req.getOperator().isEmpty())
                    ? req.getOperator() : "anonymous";
            StockRecord record = createRecord(id, "EDIT", stockBefore, stockBefore, 0,
                    operator, remark);
            if (bean.getStockRecords() == null) {
                bean.setStockRecords(new ArrayList<>());
            }
            bean.getStockRecords().add(0, record);
        }
        return repository.save(bean);
    }

    public CoffeeBean restockBean(String id, StockOperationRequest req) {
        if (req.getAmount() <= 0) {
            throw new IllegalArgumentException("Restock amount must be positive");
        }
        Optional<CoffeeBean> beanOpt = repository.findById(id);
        if (!beanOpt.isPresent()) {
            throw new IllegalArgumentException("Bean not found: " + id);
        }
        CoffeeBean bean = beanOpt.get();
        int before = bean.getStockGrams();
        int after = before + req.getAmount();
        bean.setStockGrams(after);
        bean.setLastModified(LocalDateTime.now());

        String operator = (req.getOperator() != null && !req.getOperator().isEmpty())
                ? req.getOperator() : "anonymous";
        String remark = (req.getRemark() != null) ? req.getRemark() : "";

        StockRecord record = createRecord(id, "RESTOCK", before, after, req.getAmount(),
                operator, remark);
        bean.getStockRecords().add(0, record);

        return repository.save(bean);
    }

    public CoffeeBean consumeBean(String id, StockOperationRequest req) {
        if (req.getAmount() <= 0) {
            throw new IllegalArgumentException("Consume amount must be positive");
        }
        Optional<CoffeeBean> beanOpt = repository.findById(id);
        if (!beanOpt.isPresent()) {
            throw new IllegalArgumentException("Bean not found: " + id);
        }
        CoffeeBean bean = beanOpt.get();
        int before = bean.getStockGrams();
        if (before < req.getAmount()) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        int after = before - req.getAmount();
        bean.setStockGrams(after);
        bean.setLastModified(LocalDateTime.now());

        String operator = (req.getOperator() != null && !req.getOperator().isEmpty())
                ? req.getOperator() : "anonymous";
        String remark = (req.getRemark() != null) ? req.getRemark() : "";

        StockRecord record = createRecord(id, "CONSUME", before, after, req.getAmount(),
                operator, remark);
        bean.getStockRecords().add(0, record);

        return repository.save(bean);
    }

    public boolean deleteBean(String id) {
        return repository.deleteById(id);
    }

    public List<StockRecord> getRecentRecords(String beanId, int limit) {
        Optional<CoffeeBean> beanOpt = repository.findById(beanId);
        if (!beanOpt.isPresent()) {
            return new ArrayList<>();
        }
        List<StockRecord> records = beanOpt.get().getStockRecords();
        return records.stream()
                .sorted(Comparator.comparing(StockRecord::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getWarningSummary() {
        Map<String, Object> result = new HashMap<>();
        List<CoffeeBean> all = repository.findAll();
        int approaching = 0, low = 0, empty = 0;
        List<CoffeeBean> approachingBeans = new ArrayList<>();
        List<CoffeeBean> lowBeans = new ArrayList<>();
        List<CoffeeBean> emptyBeans = new ArrayList<>();

        for (CoffeeBean b : all) {
            String level = b.getWarningLevel();
            switch (level) {
                case CoffeeBean.WARNING_APPROACHING:
                    approaching++;
                    approachingBeans.add(b);
                    break;
                case CoffeeBean.WARNING_LOW:
                    low++;
                    lowBeans.add(b);
                    break;
                case CoffeeBean.WARNING_EMPTY:
                    empty++;
                    emptyBeans.add(b);
                    break;
            }
        }

        result.put("total", all.size());
        result.put("approachingCount", approaching);
        result.put("approaching", approachingBeans);
        result.put("lowStockCount", low);
        result.put("lowStock", lowBeans);
        result.put("emptyCount", empty);
        result.put("empty", emptyBeans);
        return result;
    }

    public long getLowStockCount() {
        return repository.findAll().stream()
                .filter(CoffeeBean::isLowStock)
                .count();
    }

    public List<CoffeeBean> getLowStockBeans() {
        return repository.findAll().stream()
                .filter(CoffeeBean::isLowStock)
                .collect(Collectors.toList());
    }

    public StatisticsResponse getStatistics() {
        List<CoffeeBean> all = repository.findAll();
        StatisticsResponse resp = new StatisticsResponse();

        int totalStock = all.stream().mapToInt(CoffeeBean::getStockGrams).sum();
        resp.setTotalStockGrams(totalStock);
        resp.setTotalBeanKinds(all.size());

        int approaching = 0, low = 0, empty = 0;
        List<CoffeeBean> lowBeans = new ArrayList<>();
        for (CoffeeBean b : all) {
            String level = b.getWarningLevel();
            switch (level) {
                case CoffeeBean.WARNING_APPROACHING:
                    approaching++;
                    break;
                case CoffeeBean.WARNING_LOW:
                    low++;
                    lowBeans.add(b);
                    break;
                case CoffeeBean.WARNING_EMPTY:
                    empty++;
                    lowBeans.add(b);
                    break;
            }
        }
        resp.setApproachingCount(approaching);
        resp.setLowStockCount(low);
        resp.setEmptyCount(empty);
        resp.setLowStockBeans(lowBeans);

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Map<String, Integer> consumeMap = new HashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        for (CoffeeBean b : all) {
            nameMap.put(b.getId(), b.getName());
            for (StockRecord r : b.getStockRecords()) {
                if ("CONSUME".equals(r.getType()) && r.getTimestamp().isAfter(oneWeekAgo)) {
                    consumeMap.merge(b.getId(), r.getQuantity(), Integer::sum);
                }
            }
        }

        List<WeeklyConsumeBean> topList = consumeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> new WeeklyConsumeBean(e.getKey(), nameMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
        resp.setWeeklyTopConsumed(topList);

        return resp;
    }

    public List<CoffeeBean> importBeans(List<CoffeeBean> beans, boolean replace) {
        if (replace) {
            repository.clearAll();
        }
        List<CoffeeBean> saved = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int index = 0;
        for (CoffeeBean bean : beans) {
            index++;
            validateImportedBean(bean, index);
            if (bean.getId() == null || bean.getId().isEmpty()) {
                bean.setId(UUID.randomUUID().toString());
            }
            if (bean.getCreatedAt() == null) {
                bean.setCreatedAt(now);
            }
            bean.setLastModified(now);
            if (bean.getStockRecords() == null) {
                bean.setStockRecords(new ArrayList<>());
            }
            if (bean.getStockRecords().isEmpty() && bean.getStockGrams() > 0) {
                StockRecord record = createRecord(bean.getId(), "INIT",
                        0, bean.getStockGrams(), bean.getStockGrams(),
                        "import", "Imported from JSON/CSV");
                bean.getStockRecords().add(record);
            }
            saved.add(repository.save(bean));
        }
        return saved;
    }

    private void validateImportedBean(CoffeeBean bean, int index) {
        String prefix = "Record #" + index + ": ";
        if (bean.getName() == null || bean.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(prefix + "Bean name is required");
        }
        bean.setName(bean.getName().trim());
        if (bean.getOrigin() != null) {
            bean.setOrigin(bean.getOrigin().trim());
        }
        if (!RoastLevel.isValid(bean.getRoastLevel())) {
            throw new IllegalArgumentException(prefix + "Invalid roast level: " + bean.getRoastLevel());
        }
        if (bean.getStockGrams() < 0) {
            throw new IllegalArgumentException(prefix + "Stock grams cannot be negative: " + bean.getStockGrams());
        }
        if (bean.getMinStockLevel() < 0) {
            throw new IllegalArgumentException(prefix + "Min stock level cannot be negative: " + bean.getMinStockLevel());
        }
    }

    public void replaceAll(List<CoffeeBean> beans) {
        repository.clearAll();
        for (CoffeeBean bean : beans) {
            repository.save(bean);
        }
    }

    private StockRecord createRecord(String beanId, String type,
                                     int beforeStock, int afterStock, int quantity,
                                     String operator, String remark) {
        return new StockRecord(
                UUID.randomUUID().toString(),
                beanId,
                type,
                quantity,
                beforeStock,
                afterStock,
                operator,
                remark,
                LocalDateTime.now()
        );
    }
}
