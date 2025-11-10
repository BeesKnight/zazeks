package com.example.skilltracker.skill;

import jakarta.validation.constraints.NotBlank;

public record UpdateSkillRequest(@NotBlank(message = "Name is required") String name,
                                 String description) {
}
