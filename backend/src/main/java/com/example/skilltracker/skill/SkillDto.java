package com.example.skilltracker.skill;

import java.time.Instant;

public record SkillDto(Long id,
                       String name,
                       String description,
                       String category,
                       String color,
                       boolean archived,
                       Instant createdAt) {
}
