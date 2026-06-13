package domain;

import java.math.BigDecimal;

public class RecurringTemplate {
    private String id;
    private String name;
    private BigDecimal amount;
    private String category;
    private String payer;
    private String remark;

    public RecurringTemplate() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public RecurringTemplate(String name, BigDecimal amount, String category, String payer, String remark) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.payer = payer;
        this.remark = remark;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPayer() { return payer; }
    public void setPayer(String payer) { this.payer = payer; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
