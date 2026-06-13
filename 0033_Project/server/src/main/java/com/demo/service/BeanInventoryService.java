package com.demo.service;

import com.demo.model.CoffeeBean;
import com.demo.model.RoastLevel;
import com.demo.model.StockRecord;
import com.demo.repository.FileBeanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public List<CoffeeBean> getBeansByRoastLevel(String roastLevel) {
        return repository.findAll().stream()
                .filter(bean -> roastLevel == null || roastLevel.isEmpty() || bean.getRoastLevel().equals(roastLevel))
                .collect(Collectors.toList());
    }

    public Optional<CoffeeBean> getBeanById(String id) {
        return repository.findById(id);
    }

    public CoffeeBean addBean(CoffeeBean bean) {
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
        bean.setId(UUID.randomUUID().toString());
        bean.setCreatedAt(LocalDateTime.now());
        if (bean.getStockRecords() == null) {
            bean.setStockRecords(new ArrayList<>());
        }
        if (bean.getStockGrams() > 0) {
            StockRecord record = new StockRecord(
                    UUID.randomUUID().toString(),
                    bean.getId(),
                    "INIT",
                    bean.getStockGrams(),
                    bean.getStockGrams(),
                    LocalDateTime.now()
            );
            bean.getStockRecords().add(record);
        }
        return repository.save(bean);
    }

    public CoffeeBean restockBean(String id, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Restock amount must be positive");
        }
        Optional<CoffeeBean> beanOpt = repository.findById(id);
        if (!beanOpt.isPresent()) {
            throw new IllegalArgumentException("Bean not found: " + id);
        }
        CoffeeBean bean = beanOpt.get();
        int newStock = bean.getStockGrams() + amount;
        bean.setStockGrams(newStock);

        StockRecord record = new StockRecord(
                UUID.randomUUID().toString(),
                bean.getId(),
                "RESTOCK",
                amount,
                newStock,
                LocalDateTime.now()
        );
        bean.getStockRecords().add(0, record);

        return repository.save(bean);
    }

    public CoffeeBean consumeBean(String id, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Consume amount must be positive");
        }
        Optional<CoffeeBean> beanOpt = repository.findById(id);
        if (!beanOpt.isPresent()) {
            throw new IllegalArgumentException("Bean not found: " + id);
        }
        CoffeeBean bean = beanOpt.get();
        if (bean.getStockGrams() < amount) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        int newStock = bean.getStockGrams() - amount;
        bean.setStockGrams(newStock);

        StockRecord record = new StockRecord(
                UUID.randomUUID().toString(),
                bean.getId(),
                "CONSUME",
                amount,
                newStock,
                LocalDateTime.now()
        );
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
}
