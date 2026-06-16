package app;

import model.DisposePlan;
import model.HouseholdItem;
import model.ItemStatus;
import persist.ItemJsonStore;

import java.math.BigDecimal;
import java.util.*;

public class DeclutterService {
    private final ItemJsonStore store;

    public DeclutterService(ItemJsonStore store) {
        this.store = store;
        normalizeAllItems();
    }

    private void normalizeAllItems() {
        int fixed = 0;
        for (HouseholdItem item : store.findAll()) {
            if (item.getStatus() == null) {
                item.setStatus(ItemStatus.getDefaultForPlan(item.getDisposePlan()));
                store.updateStatus(item.getId(), item.getStatus());
                fixed++;
                continue;
            }
            if (!item.getStatus().isCompatibleWith(item.getDisposePlan())) {
                ItemStatus fixedStatus = ItemStatus.getDefaultForPlan(item.getDisposePlan());
                item.setStatus(fixedStatus);
                store.updateStatus(item.getId(), fixedStatus);
                fixed++;
            }
        }
        if (fixed > 0) {
            System.out.println("[DeclutterService] 已自动修正 " + fixed + " 条不一致的状态数据");
        }
    }

    public List<HouseholdItem> listItems(String categoryFilter, String disposePlanFilter) {
        return searchItems(null, categoryFilter, disposePlanFilter, null, null, null, "createdAt", "desc");
    }

    public List<HouseholdItem> searchItems(String keyword, String category, String disposePlan,
                                           String status, BigDecimal minPrice, BigDecimal maxPrice,
                                           String sortBy, String sortOrder) {
        List<HouseholdItem> all = store.findAll();
        List<HouseholdItem> result = new ArrayList<>();

        DisposePlan planFilter = null;
        if (disposePlan != null && !disposePlan.isEmpty() && !"ALL".equalsIgnoreCase(disposePlan)) {
            try {
                planFilter = DisposePlan.fromNameOrDisplayName(disposePlan);
            } catch (Exception ignored) {
            }
        }

        ItemStatus statusFilter = null;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusFilter = ItemStatus.fromNameOrDisplayName(status);
            } catch (Exception ignored) {
            }
        }

        String kw = (keyword != null) ? keyword.trim().toLowerCase() : "";

        for (HouseholdItem item : all) {
            if (category != null && !category.isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                if (!category.equals(item.getCategory())) continue;
            }
            if (planFilter != null && !planFilter.equals(item.getDisposePlan())) continue;
            if (statusFilter != null && !statusFilter.equals(item.getStatus())) continue;

            if (!kw.isEmpty()) {
                boolean match = false;
                if (item.getName() != null && item.getName().toLowerCase().contains(kw)) match = true;
                if (!match && item.getLocation() != null && item.getLocation().toLowerCase().contains(kw)) match = true;
                if (!match && item.getRemark() != null && item.getRemark().toLowerCase().contains(kw)) match = true;
                if (!match && item.getCategory() != null && item.getCategory().toLowerCase().contains(kw)) match = true;
                if (!match) continue;
            }

            if (minPrice != null && item.getEstimatedPrice() != null) {
                if (item.getEstimatedPrice().compareTo(minPrice) < 0) continue;
            }
            if (maxPrice != null && item.getEstimatedPrice() != null) {
                if (item.getEstimatedPrice().compareTo(maxPrice) > 0) continue;
            }

            result.add(item);
        }

        sortItems(result, sortBy, sortOrder);
        return result;
    }

    private void sortItems(List<HouseholdItem> items, String sortBy, String sortOrder) {
        final boolean asc = "asc".equalsIgnoreCase(sortOrder);
        final String field = (sortBy != null) ? sortBy : "createdAt";

        Collections.sort(items, new Comparator<HouseholdItem>() {
            @Override
            public int compare(HouseholdItem a, HouseholdItem b) {
                int cmp = 0;
                if ("name".equals(field)) {
                    String na = a.getName() != null ? a.getName() : "";
                    String nb = b.getName() != null ? b.getName() : "";
                    cmp = na.compareTo(nb);
                } else if ("price".equals(field)) {
                    BigDecimal pa = a.getEstimatedPrice() != null ? a.getEstimatedPrice() : BigDecimal.ZERO;
                    BigDecimal pb = b.getEstimatedPrice() != null ? b.getEstimatedPrice() : BigDecimal.ZERO;
                    cmp = pa.compareTo(pb);
                } else if ("category".equals(field)) {
                    String ca = a.getCategory() != null ? a.getCategory() : "";
                    String cb = b.getCategory() != null ? b.getCategory() : "";
                    cmp = ca.compareTo(cb);
                } else if ("updatedAt".equals(field)) {
                    cmp = Long.compare(a.getUpdatedAt(), b.getUpdatedAt());
                } else {
                    cmp = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                }
                return asc ? cmp : -cmp;
            }
        });
    }

    public HouseholdItem addItem(HouseholdItem item) {
        if (item.getStatus() == null) {
            item.setStatus(ItemStatus.getDefaultForPlan(item.getDisposePlan()));
        } else if (!item.getStatus().isCompatibleWith(item.getDisposePlan())) {
            item.setStatus(ItemStatus.getDefaultForPlan(item.getDisposePlan()));
        }
        store.add(item);
        return item;
    }

    public boolean updateDisposePlan(String id, DisposePlan plan) {
        HouseholdItem item = store.findById(id);
        if (item == null) return false;
        ItemStatus currentStatus = item.getStatus();
        boolean ok = store.updateDisposePlan(id, plan);
        if (ok && currentStatus != null && !currentStatus.isCompatibleWith(plan)) {
            ItemStatus newStatus = ItemStatus.getDefaultForPlan(plan);
            store.updateStatus(id, newStatus);
        }
        return ok;
    }

    public boolean updateStatus(String id, ItemStatus status) {
        HouseholdItem item = store.findById(id);
        if (item == null) return false;
        if (status != null && !status.isCompatibleWith(item.getDisposePlan())) {
            return false;
        }
        return store.updateStatus(id, status);
    }

    public boolean updateImageUrl(String id, String imageUrl) {
        return store.updateImageUrl(id, imageUrl);
    }

    public int batchUpdateDisposePlan(List<String> ids, DisposePlan plan) {
        int updated = store.batchUpdateDisposePlan(ids, plan);
        if (updated > 0) {
            ItemStatus defaultStatus = ItemStatus.getDefaultForPlan(plan);
            List<String> toFix = new ArrayList<>();
            for (String id : ids) {
                HouseholdItem item = store.findById(id);
                if (item != null && item.getStatus() != null && !item.getStatus().isCompatibleWith(plan)) {
                    toFix.add(id);
                }
            }
            if (!toFix.isEmpty()) {
                store.batchUpdateStatus(toFix, defaultStatus);
            }
        }
        return updated;
    }

    public int batchUpdateStatus(List<String> ids, ItemStatus status) {
        List<String> validIds = new ArrayList<>();
        for (String id : ids) {
            HouseholdItem item = store.findById(id);
            if (item != null && (status == null || status.isCompatibleWith(item.getDisposePlan()))) {
                validIds.add(id);
            }
        }
        return store.batchUpdateStatus(validIds, status);
    }

    public int batchDelete(List<String> ids) {
        return store.batchDelete(ids);
    }

    public boolean deleteItem(String id) {
        return store.delete(id);
    }

    public HouseholdItem findItem(String id) {
        return store.findById(id);
    }

    public BigDecimal calculateExpectedRevenue() {
        BigDecimal total = BigDecimal.ZERO;
        for (HouseholdItem item : store.findAll()) {
            if (item.getDisposePlan() == DisposePlan.SELL
                    && item.getEstimatedPrice() != null
                    && item.getStatus() != ItemStatus.SOLD) {
                total = total.add(item.getEstimatedPrice());
            }
        }
        return total;
    }

    public BigDecimal calculateSoldRevenue() {
        BigDecimal total = BigDecimal.ZERO;
        for (HouseholdItem item : store.findAll()) {
            if (item.getStatus() == ItemStatus.SOLD && item.getEstimatedPrice() != null) {
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

    public int countByStatus(ItemStatus status) {
        int count = 0;
        for (HouseholdItem item : store.findAll()) {
            if (item.getStatus() == status) count++;
        }
        return count;
    }

    public Map<String, Integer> countByCategory() {
        Map<String, Integer> map = new TreeMap<>();
        for (HouseholdItem item : store.findAll()) {
            String cat = (item.getCategory() != null && !item.getCategory().isEmpty()) ? item.getCategory() : "未分类";
            Integer count = map.get(cat);
            map.put(cat, count == null ? 1 : count + 1);
        }
        return map;
    }

    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        int total = countItems();
        stats.put("totalItems", total);
        stats.put("expectedRevenue", calculateExpectedRevenue());
        stats.put("soldRevenue", calculateSoldRevenue());

        Map<String, Integer> planCounts = new LinkedHashMap<>();
        for (DisposePlan plan : DisposePlan.values()) {
            planCounts.put(plan.name(), countByDisposePlan(plan));
        }
        stats.put("disposePlanCounts", planCounts);

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (ItemStatus status : ItemStatus.values()) {
            statusCounts.put(status.name(), countByStatus(status));
        }
        stats.put("statusCounts", statusCounts);

        stats.put("categories", new ArrayList<>(listCategories()));
        stats.put("categoryCounts", countByCategory());

        return stats;
    }

    public String exportCsv(List<HouseholdItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,名称,分类,处理方式,状态,预估价格,存放位置,备注,创建时间,更新时间\n");
        for (HouseholdItem item : items) {
            sb.append(csvCell(item.getId())).append(",");
            sb.append(csvCell(item.getName())).append(",");
            sb.append(csvCell(item.getCategory())).append(",");
            sb.append(csvCell(item.getDisposePlan() != null ? item.getDisposePlan().getDisplayName() : "")).append(",");
            sb.append(csvCell(item.getStatus() != null ? item.getStatus().getDisplayName() : "")).append(",");
            sb.append(item.getEstimatedPrice() != null ? item.getEstimatedPrice().toPlainString() : "0").append(",");
            sb.append(csvCell(item.getLocation())).append(",");
            sb.append(csvCell(item.getRemark())).append(",");
            sb.append(item.getCreatedAt()).append(",");
            sb.append(item.getUpdatedAt()).append("\n");
        }
        return sb.toString();
    }

    public String exportJson(List<HouseholdItem> items) {
        return ItemJsonStore.serializeList(items);
    }

    private static String csvCell(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
