package domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Expense {
    private String id;
    private BigDecimal amount;
    private String category;
    private String payer;
    private String remark;
    private LocalDateTime time;

    public Expense() {
    }

    public Expense(BigDecimal amount, String category, String payer, String remark, LocalDateTime time) {
        this.id = UUID.randomUUID().toString();
        this.amount = amount;
        this.category = category;
        this.payer = payer;
        this.remark = remark;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
