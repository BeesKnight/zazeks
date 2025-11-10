package com.example.skilltracker.stats;

public record InactiveSkillDto(Long skillId,
                               String skillName,
                               long daysSinceLastSession) {
}
