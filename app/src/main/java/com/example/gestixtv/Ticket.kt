package com.example.gestixtv

data class Ticket(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val prioridad: String,
    val departamentoId: Int,
    val estado: String = "abierto", // abierto | pendiente | resuelto
    val asignadoAId: Int? = null,
    val fechaCreacion: String = ""
)

data class Department(
    val id: Int,
    val nombre: String
)
