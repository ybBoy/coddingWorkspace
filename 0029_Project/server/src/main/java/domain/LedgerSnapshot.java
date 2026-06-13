package domain;

import java.util.ArrayList;
import java.util.List;

public class LedgerSnapshot {
    private List<Expense> expenses;
    private List<Budget> budgets;
    private LedgerConfig config;
    private List<RecurringTemplate> templates;

    public LedgerSnapshot() {
        this.expenses = new ArrayList<>();
        this.budgets = new ArrayList<>();
        this.config = new LedgerConfig();
        this.templates = new ArrayList<>();
    }

    public LedgerSnapshot(List<Expense> expenses, List<Budget> budgets, LedgerConfig config) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.config = config != null ? config : new LedgerConfig();
        this.templates = new ArrayList<>();
    }

    public LedgerSnapshot(List<Expense> expenses, List<Budget> budgets, LedgerConfig config, List<RecurringTemplate> templates) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.config = config != null ? config : new LedgerConfig();
        this.templates = templates != null ? templates : new ArrayList<>();
    }

    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
    public List<Budget> getBudgets() { return budgets; }
    public void setBudgets(List<Budget> budgets) { this.budgets = budgets; }
    public LedgerConfig getConfig() { return config; }
    public void setConfig(LedgerConfig config) { this.config = config; }
    public List<RecurringTemplate> getTemplates() { return templates; }
    public void setTemplates(List<RecurringTemplate> templates) { this.templates = templates; }
}
