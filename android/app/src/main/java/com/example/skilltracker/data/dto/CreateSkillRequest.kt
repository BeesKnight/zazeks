package com.example.skilltracker.data.dto

data class CreateSkillRequest(
    val name: String,
    val description: String?,
    val category: String?,
    val color: String?
)
