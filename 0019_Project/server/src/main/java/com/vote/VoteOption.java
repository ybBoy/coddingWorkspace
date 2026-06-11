package com.vote;

/**
 * VoteOption 职责：
 * 投票选项的 Java 实体类，对应内存和 JSON 文件中的一条数据。
 * 包含选项 ID、名称、票数三个字段。
 */
public class VoteOption {
    private String id;
    private String name;
    private int votes;

    public VoteOption() {
    }

    public VoteOption(String id, String name, int votes) {
        this.id = id;
        this.name = name;
        this.votes = votes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }
}
