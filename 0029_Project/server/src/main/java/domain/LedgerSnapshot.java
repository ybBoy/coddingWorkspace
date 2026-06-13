package domain;

import java.util.ArrayList;
import java.util.List;

public class LedgerSnapshot {
    private List<Expense> expenses;
    private List<Budget> budgets;

    public LedgerSnapshot() {
        this.expenses = new ArrayList<>();
        this.budgets = new ArrayList<>();
    }

    public LedgerSnapshot(List<Expense> expenses, List<Budget> budgets) {
        this.expenses = expenses;
        this.budgets = budgets;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public List<Budget> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<Budget> budgets) {
        this.budgets = budgets;
    }
}
