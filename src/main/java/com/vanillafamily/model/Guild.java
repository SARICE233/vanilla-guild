package com.vanillafamily.model;

import java.sql.Timestamp;

public class Guild {

    private int id;
    private String name;
    private String tag;
    private String description;
    private String leaderUuid;
    private int level;
    private int experience;
    private int bankRows;
    private Timestamp createdAt;

    public Guild() {}

    public Guild(String name, String leaderUuid) {
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.level = 1;
        this.experience = 0;
        this.bankRows = 1;
    }

    // === Getters & Setters ===
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLeaderUuid() { return leaderUuid; }
    public void setLeaderUuid(String leaderUuid) { this.leaderUuid = leaderUuid; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getBankRows() { return bankRows; }
    public void setBankRows(int bankRows) { this.bankRows = bankRows; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
