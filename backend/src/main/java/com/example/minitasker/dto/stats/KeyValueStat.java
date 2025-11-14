package com.example.minitasker.dto.stats;

public class KeyValueStat {
    private String key;
    private long value;

    public KeyValueStat() {
    }

    public KeyValueStat(String key, long value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
