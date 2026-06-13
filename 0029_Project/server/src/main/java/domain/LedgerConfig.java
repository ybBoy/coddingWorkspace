package domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LedgerConfig {
    private String ledgerName;
    private List<String> categories;
    private List<String> payers;

    public LedgerConfig() {
        this.ledgerName = "🏠 家庭账本";
        this.categories = new ArrayList<>(Arrays.asList(
                "餐饮", "购物", "交通", "娱乐", "医疗", "教育", "住房", "通讯", "其他"
        ));
        this.payers = new ArrayList<>(Arrays.asList("爸爸", "妈妈", "孩子", "共同"));
    }

    public LedgerConfig(String ledgerName, List<String> categories, List<String> payers) {
        this.ledgerName = ledgerName;
        this.categories = categories;
        this.payers = payers;
    }

    public String getLedgerName() {
        return ledgerName;
    }

    public void setLedgerName(String ledgerName) {
        this.ledgerName = ledgerName;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getPayers() {
        return payers;
    }

    public void setPayers(List<String> payers) {
        this.payers = payers;
    }
}
