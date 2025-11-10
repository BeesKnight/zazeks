package com.example.skilltracker.skill;

public record UpdateSkillRequest(String name,
                                 String description,
                                 String category,
                                 String color,
                                 Boolean archived) {
}
