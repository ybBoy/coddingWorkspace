package app;

import model.DisposePlan;
import model.HouseholdItem;
import persist.ItemJsonStore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DeclutterService {
    private final ItemJsonStore store;

    public DeclutterService(ItemJsonStore store) {
        this.store = store;
    }

    public List<HouseholdItem> listItems(String categoryFilter, String disposePlanFilter) {
        List<HouseholdItem> all = store.findAll();
        List<HouseholdItem> result = new ArrayList<>();
        DisposePlan planFilter = null;
        if (disposePlanFilter != null && !disposePlanFilter.isEmpty() && !"ALL".equalsIgnoreCase(disposePlanFilter)) {
            try {
                planFilter = DisposePlan.fromNameOrDisplayName(disposePlanFilter);
            } catch (Exception ignored) {
            }
        }
        for (HouseholdItem item : all) {
            if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equalsIgnoreCase(categoryFilter)) {
                if (!categoryFilter.equals(item.getCategory())) {
                    continue;
                }
            }
            if (planFilter != null && !planFilter.equals(item.getDisposePlan())) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    public HouseholdItem addItem(HouseholdItem item) {
        store.add(item);
        return item;
    }

    public boolean updateDisposePlan(String id, DisposePlan plan) {
        return store.updateDisposePlan(id, plan);
    }

    public boolean deleteItem(String id) {
        return store.delete(id);
    }

    public BigDecimal calculateExpectedRevenue() {
        BigDecimal total = BigDecimal.ZERO;
        for (HouseholdItem item : store.findAll()) {
            if (item.getDisposePlan() == DisposePlan.SELL && item.getEstimatedPrice() != null) {
                total = total.add(item.getEstimatedPrice());
            }
        }
        return total;
    }

    public Set<String> listCategories() {
        Set<String> categories = new TreeSet<>();
        for (HouseholdItem item : store.findAll()) {
            if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                categories.add(item.getCategory());
            }
        }
        return categories;
    }

    public int countItems() {
        return store.findAll().size();
    }

    public int countByDisposePlan(DisposePlan plan) {
        int count = 0;
        for (HouseholdItem item : store.findAll()) {
            if (item.getDisposePlan() == plan) count++;
        }
        return count;
    }
}
