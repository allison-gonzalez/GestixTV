package com.example.gestixtv

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginResult(
    val success: Boolean,
    val departamentoId: Int = -1,
    val token: String? = null,
    val error: String = ""
)

object ApiService {

    private const val BASE_URL = "https://gestix-backend-express.onrender.com/api"
    const val SOCKET_URL = "https://gestix-backend-express.onrender.com"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun realizarLogin(email: String, pass: String): LoginResult {
        val json = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)

            if (response.isSuccessful) {
                val user = jsonResponse.optJSONObject("user")
                val deptoId = user?.optInt("departamento_id", -1) ?: -1
                val token = jsonResponse.optString("access_token").ifBlank { null }
                LoginResult(success = true, departamentoId = deptoId, token = token)
            } else {
                val mensaje = jsonResponse.optString("message", "Credenciales incorrectas")
                LoginResult(success = false, error = mensaje)
            }
        } catch (e: Exception) {
            LoginResult(success = false, error = "Error de conexión: ${e.message}")
        }
    }

    fun obtenerDepartamentos(): List<Department> {
        val request = Request.Builder().url("$BASE_URL/departamentos").build()
        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val jsonArray = JSONObject(response.body?.string() ?: "").getJSONArray("data")
            val departamentos = mutableListOf<Department>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                departamentos.add(Department(id = item.optInt("id"), nombre = item.optString("nombre", "Departamento")))
            }
            departamentos
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun obtenerTickets(token: String?, userDepartamentoId: Int): List<Ticket> {
        val requestBuilder = Request.Builder().url("$BASE_URL/tickets")
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) return emptyList()

        val jsonArray = JSONObject(response.body?.string() ?: "").getJSONArray("data")
        val nuevosTickets = mutableListOf<Ticket>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val deptoId = item.optInt("departamento_id", -1)

            if (deptoId == userDepartamentoId || userDepartamentoId == -1) {
                nuevosTickets.add(
                    Ticket(
                        id = item.optInt("id"),
                        titulo = item.optString("titulo", "Sin título"),
                        descripcion = item.optString("descripcion", "Sin descripción"),
                        prioridad = item.optString("prioridad", "media"),
                        departamentoId = deptoId,
                        estado = item.optString("estado", "abierto"),
                        asignadoAId = item.optInt("asignado_a_id", -1).takeIf { it != -1 },
                        fechaCreacion = item.optString("fecha_creacion", "")
                    )
                )
            }
        }
        return nuevosTickets
    }
}
