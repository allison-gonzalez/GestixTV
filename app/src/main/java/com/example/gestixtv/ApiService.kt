package com.example.gestixtv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiService {

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Traduce el ID numérico al nombre real del departamento de MongoDB
    fun obtenerNombreDepartamento(id: Int): String {
        return when (id) {
            1 -> "Soporte Técnico"
            2 -> "Recursos Humanos"
            3 -> "Finanzas"
            4 -> "Operaciones"
            5 -> "Sistemas"
            6 -> "Seguridad"
            7 -> "Viaticos"
            else -> "General / Todos"
        }
    }

    fun realizarLogin(email: String, pass: String, callback: (Boolean, Int, String) -> Unit) {
        val client = getUnsafeOkHttpClient()
        val json = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://backend-movil-4x6m.onrender.com/api/auth/login")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                var deptoId = -1

                if (jsonResponse.has("user")) {
                    deptoId = jsonResponse.getJSONObject("user").optInt("departamento_id", -1)
                } else if (jsonResponse.has("data")) {
                    val dataObj = jsonResponse.getJSONObject("data")
                    deptoId = if (dataObj.has("user")) {
                        dataObj.getJSONObject("user").optInt("departamento_id", -1)
                    } else {
                        dataObj.optInt("departamento_id", -1)
                    }
                } else {
                    deptoId = jsonResponse.optInt("departamento_id", -1)
                }

                callback(true, deptoId, "")
            } else {
                callback(false, -1, "Credenciales incorrectas")
            }
        } catch (e: Exception) {
            callback(false, -1, "Error de conexión: ${e.message}")
        }
    }

    fun obtenerTickets(userDepartamentoId: Int): List<Ticket> {
        val client = getUnsafeOkHttpClient()
        val request = Request.Builder()
            .url("https://backend-movil-4x6m.onrender.com/api/tickets")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val jsonCrudo = response.body?.string() ?: ""
        val jsonObject = JSONObject(jsonCrudo)
        val jsonArray = jsonObject.getJSONArray("data")
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
                        prioridad = item.optString("prioridad", "Normal"),
                        departamentoId = deptoId
                    )
                )
            }
        }
        return nuevosTickets
    }
}