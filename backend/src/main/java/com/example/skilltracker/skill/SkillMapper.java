package com.example.skilltracker.skill;

public class SkillMapper {

    private SkillMapper() {
    }

    public static SkillDto toDto(SkillEntity entity) {
        return new SkillDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getColor(),
                entity.isArchived(),
                entity.getCreatedAt()
        );
    }

    public static void updateEntity(SkillEntity entity, UpdateSkillRequest request) {
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
        if (request.color() != null) {
            entity.setColor(request.color());
        }
        if (request.archived() != null) {
            entity.setArchived(request.archived());
        }
    }
}
