package com.example.mentalmathbattle.model

data class SocketMessage(
    val type: String,
    val userId: String?,
    val message: String?,
    val question: String?,
    val answer: Int?,
    val correct: Boolean?,
    val correctAnswer: Int?
)
