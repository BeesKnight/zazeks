package com.example.skilltracker.data.dto

data class UpdateSkillRequest(
    val name: String?,
    val description: String?,
    val category: String?,
    val color: String?,
    val archived: Boolean?
)
