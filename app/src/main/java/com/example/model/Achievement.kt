package com.example.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val progress: Int,
    val maxProgress: Int,
    val unlockedDate: Long? = null
)
