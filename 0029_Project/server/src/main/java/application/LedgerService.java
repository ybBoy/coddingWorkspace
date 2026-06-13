package application;

import domain.Budget;
import domain.Expense;
import domain.LedgerSnapshot;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class LedgerService {
    private final List<Expense> expenses;
    private final Map<String, Budget> budgets;

    public LedgerService() {
        this.expenses = new CopyOnWriteArrayList<>();
        this.budgets = new ConcurrentHashMap<>();
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
    }

    public LedgerSnapshot createSnapshot() {
        return new LedgerSnapshot(
                new ArrayList<>(expenses),
                new ArrayList<>(budgets.values())
        );
    }

    public Expense addExpense(Expense expense) {
        expenses.add(expense);
        return expense;
    }

    public boolean deleteExpense(String id) {
        return expenses.removeIf(e -> e.getId().equals(id));
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
}
