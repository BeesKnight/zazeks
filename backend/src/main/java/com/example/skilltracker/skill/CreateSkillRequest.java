package com.example.skilltracker.skill;

import jakarta.validation.constraints.NotBlank;

public record CreateSkillRequest(@NotBlank(message = "Name is required") String name,
                                 String description) {
}
