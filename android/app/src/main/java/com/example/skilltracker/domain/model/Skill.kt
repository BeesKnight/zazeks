package com.example.skilltracker.domain.model

data class Skill(
    val id: Long,
    val name: String,
    val description: String?,
    val category: String?,
    val color: String?,
    val archived: Boolean,
    val createdAt: String
)
