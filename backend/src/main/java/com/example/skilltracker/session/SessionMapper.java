package com.example.skilltracker.session;

public class SessionMapper {

    private SessionMapper() {
    }

    public static SessionDto toDto(SessionEntity entity) {
        return new SessionDto(entity.getId(), entity.getSkill().getId(), entity.getSessionDate(), entity.getDurationMinutes(), entity.getNotes());
    }
}
