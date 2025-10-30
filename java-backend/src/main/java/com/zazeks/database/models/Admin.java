package com.zazeks.database.models;

/**
 * Аналог модели {@code Admin} из Python-проекта. Хранит связь пользователя с правами администратора.
 */
public class Admin {
    private Integer id;
    private final int userId;

    public Admin(Integer id, int userId) {
        this.id = id;
        this.userId = userId;
    }

    public Admin(int userId) {
        this(null, userId);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }
}
