package com.example.skilltracker.skill;

public class SkillMapper {

    private SkillMapper() {
    }

    public static SkillDto toDto(SkillEntity entity) {
        return new SkillDto(entity.getId(), entity.getName(), entity.getDescription(), entity.getCreatedAt());
    }

    public static void updateEntity(SkillEntity entity, UpdateSkillRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
    }
}
