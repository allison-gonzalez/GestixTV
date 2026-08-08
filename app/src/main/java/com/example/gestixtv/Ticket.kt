package com.example.gestixtv

data class Ticket(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val prioridad: String,
    val departamentoId: Int
)