package com.vanillafamily.model;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuildTask {
    private int id;
    private String name;
    private String description;
    private int target;          // 目标击杀数
    private int reward;          // 报酬金币
    private int completed;       // 0=进行中, 1=已完成
    private int winnerGuildId;   // 获胜公会ID
    private Timestamp createdAt;
    // 内存中的进度: guildId -> count (不持久化到task表，存在task_progress表)
    private Map<Integer, Integer> progress = new ConcurrentHashMap<>();

    public GuildTask() {}

    public GuildTask(String name, int target, int reward) {
        this.name = name;
        this.target = target;
        this.reward = reward;
        this.description = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    public int getReward() { return reward; }
    public void setReward(int reward) { this.reward = reward; }
    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }
    public int getWinnerGuildId() { return winnerGuildId; }
    public void setWinnerGuildId(int winnerGuildId) { this.winnerGuildId = winnerGuildId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Map<Integer, Integer> getProgress() { return progress; }
}
