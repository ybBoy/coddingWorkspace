package application;

import domain.Budget;
import domain.Expense;
import domain.LedgerConfig;
import domain.LedgerSnapshot;
import domain.RecurringTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class LedgerService {
    private final List<Expense> expenses;
    private final Map<String, Budget> budgets;
    private final List<RecurringTemplate> templates;
    private LedgerConfig config;

    public LedgerService() {
        this.expenses = new CopyOnWriteArrayList<>();
        this.budgets = new ConcurrentHashMap<>();
        this.templates = new CopyOnWriteArrayList<>();
        this.config = new LedgerConfig();
    }

    public void restoreFromSnapshot(LedgerSnapshot snapshot) {
        expenses.clear();
        if (snapshot.getExpenses() != null) {
            expenses.addAll(snapshot.getExpenses());
        }
        budgets.clear();
        if (snapshot.getBudgets() != null) {
            for (Budget budget : snapshot.getBudgets()) {
                budgets.put(budget.getCategory(), budget);
            }
        }
        templates.clear();
        if (snapshot.getTemplates() != null) {
            templates.addAll(snapshot.getTemplates());
        }
        if (snapshot.getConfig() != null) {
            this.config = snapshot.getConfig();
        } else {
            this.config = new LedgerConfig();
        }
    }

    public LedgerSnapshot createSnapshot() {
        return new LedgerSnapshot(
                new ArrayList<>(expenses),
                new ArrayList<>(budgets.values()),
                config,
                new ArrayList<>(templates)
        );
    }

    public Expense addExpense(Expense expense) {
        expenses.add(expense);
        return expense;
    }

    public boolean deleteExpense(String id) {
        return expenses.removeIf(e -> e.getId().equals(id));
    }

    public Expense editExpense(String id, BigDecimal amount, String category, String payer, String remark, String timeStr) {
        for (Expense expense : expenses) {
            if (expense.getId().equals(id)) {
                expense.setAmount(amount);
                expense.setCategory(category);
                expense.setPayer(payer);
                expense.setRemark(remark);
                if (timeStr != null && !timeStr.isEmpty()) {
                    expense.setTime(LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                return expense;
            }
        }
        return null;
    }

    public void setBudget(Budget budget) {
        if (budget.getAmount() == null || budget.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            budgets.remove(budget.getCategory());
        } else {
            budgets.put(budget.getCategory(), budget);
        }
    }

    public boolean removeBudget(String category) {
        return budgets.remove(category) != null;
    }

    public List<Expense> getExpensesByMonth(YearMonth month) {
        return expenses.stream()
                .filter(e -> YearMonth.from(e.getTime()).equals(month))
                .sorted((a, b) -> b.getTime().compareTo(a.getTime()))
                .collect(Collectors.toList());
    }

    public BigDecimal getMonthlyTotal(YearMonth month) {
        return getExpensesByMonth(month).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> getCategoryTotals(YearMonth month) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : getExpensesByMonth(month)) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Map<String, BigDecimal> getCategoryTotalsWithBudgets(YearMonth month) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Budget budget : budgets.values()) {
            totals.put(budget.getCategory(), BigDecimal.ZERO);
        }
        for (Expense e : getExpensesByMonth(month)) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted((a, b) -> {
                    boolean aHasBudget = budgets.containsKey(a.getKey());
                    boolean bHasBudget = budgets.containsKey(b.getKey());
                    if (aHasBudget && !bHasBudget) return -1;
                    if (!aHasBudget && bHasBudget) return 1;
                    return b.getValue().compareTo(a.getValue());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public BigDecimal getBudgetForCategory(String category) {
        Budget budget = budgets.get(category);
        return budget != null ? budget.getAmount() : BigDecimal.ZERO;
    }

    public List<Budget> getAllBudgets() {
        return new ArrayList<>(budgets.values());
    }

    public List<Expense> getRecentExpenses(int limit) {
        return expenses.stream()
                .sorted((a, b) -> b.getTime().compareTo(a.getTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getPayerTotals(YearMonth month) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : getExpensesByMonth(month)) {
            totals.merge(e.getPayer(), e.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public LedgerConfig getConfig() {
        return config;
    }

    public void updateConfig(LedgerConfig newConfig) {
        if (newConfig.getLedgerName() != null && !newConfig.getLedgerName().trim().isEmpty()) {
            this.config.setLedgerName(newConfig.getLedgerName());
        }
        if (newConfig.getCategories() != null && !newConfig.getCategories().isEmpty()) {
            this.config.setCategories(new ArrayList<>(newConfig.getCategories()));
        }
        if (newConfig.getPayers() != null && !newConfig.getPayers().isEmpty()) {
            this.config.setPayers(new ArrayList<>(newConfig.getPayers()));
        }
    }

    public RecurringTemplate addTemplate(RecurringTemplate template) {
        templates.add(template);
        return template;
    }

    public boolean deleteTemplate(String id) {
        return templates.removeIf(t -> t.getId().equals(id));
    }

    public List<RecurringTemplate> getAllTemplates() {
        return new ArrayList<>(templates);
    }

    public List<String> applyTemplates(YearMonth month) {
        List<String> addedIds = new ArrayList<>();
        for (RecurringTemplate t : templates) {
            Expense expense = new Expense(t.getAmount(), t.getCategory(), t.getPayer(), t.getRemark(), month.atDay(1).atStartOfDay());
            expenses.add(expense);
            addedIds.add(expense.getId());
        }
        return addedIds;
    }

    public Map<String, Object> getMonthComparison(YearMonth current, YearMonth previous) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal currentTotal = getMonthlyTotal(current);
        BigDecimal prevTotal = getMonthlyTotal(previous);
        result.put("currentTotal", currentTotal.toString());
        result.put("previousTotal", prevTotal.toString());
        result.put("totalDiff", currentTotal.subtract(prevTotal).toString());

        Map<String, BigDecimal> currentCats = getCategoryTotals(current);
        Map<String, BigDecimal> prevCats = getCategoryTotals(previous);
        Set<String> allCats = new LinkedHashSet<>();
        allCats.addAll(prevCats.keySet());
        allCats.addAll(currentCats.keySet());
        Map<String, Map<String, String>> categoryChanges = new LinkedHashMap<>();
        for (String cat : allCats) {
            BigDecimal cur = currentCats.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal prev = prevCats.getOrDefault(cat, BigDecimal.ZERO);
            Map<String, String> change = new LinkedHashMap<>();
            change.put("current", cur.toString());
            change.put("previous", prev.toString());
            change.put("diff", cur.subtract(prev).toString());
            categoryChanges.put(cat, change);
        }
        result.put("categoryChanges", categoryChanges);

        Map<String, BigDecimal> currentPayers = getPayerTotals(current);
        Map<String, BigDecimal> prevPayers = getPayerTotals(previous);
        Set<String> allPayers = new LinkedHashSet<>();
        allPayers.addAll(prevPayers.keySet());
        allPayers.addAll(currentPayers.keySet());
        Map<String, Map<String, String>> payerChanges = new LinkedHashMap<>();
        for (String payer : allPayers) {
            BigDecimal cur = currentPayers.getOrDefault(payer, BigDecimal.ZERO);
            BigDecimal prev = prevPayers.getOrDefault(payer, BigDecimal.ZERO);
            Map<String, String> change = new LinkedHashMap<>();
            change.put("current", cur.toString());
            change.put("previous", prev.toString());
            change.put("diff", cur.subtract(prev).toString());
            payerChanges.put(payer, change);
        }
        result.put("payerChanges", payerChanges);

        return result;
    }

    public List<Map<String, Object>> getBudgetTrend(int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            YearMonth ym = start.plusMonths(i);
            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("year", ym.getYear());
            monthData.put("month", ym.getMonthValue());
            Map<String, BigDecimal> catTotals = getCategoryTotals(ym);
            List<Map<String, Object>> catTrends = new ArrayList<>();
            for (Budget budget : budgets.values()) {
                BigDecimal spent = catTotals.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
                Map<String, Object> ct = new LinkedHashMap<>();
                ct.put("category", budget.getCategory());
                ct.put("budget", budget.getAmount().toString());
                ct.put("spent", spent.toString());
                ct.put("ratio", budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                        ? spent.multiply(new BigDecimal("100")).divide(budget.getAmount(), 2, java.math.RoundingMode.HALF_UP).doubleValue()
                        : 0.0);
                catTrends.add(ct);
            }
            monthData.put("categories", catTrends);
            trend.add(monthData);
        }
        return trend;
    }
}
