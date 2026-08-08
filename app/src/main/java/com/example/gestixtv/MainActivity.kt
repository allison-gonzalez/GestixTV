package com.example.gestixtv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.OutlinedTextFieldDefaults

val BrandDarkBlue = Color(0xFF213C58)
val BrandYellow = Color(0xFFFFD600)
val BrandLightBg = Color(0xFFF3F4F6)

class MainActivity : ComponentActivity() {

    private val ticketsList = mutableStateListOf<Ticket>()
    var estadoMensaje by mutableStateOf("Conectando con el servidor...")
    var cargando by mutableStateOf(true)

    var isLoggedIn by mutableStateOf(false)
    var userDepartamentoId by mutableStateOf(-1)

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
        val savedDepto = sharedPref.getInt("departamento_id", -1)

        if (savedDepto != -1) {
            userDepartamentoId = savedDepto
            isLoggedIn = true
            iniciarSimulacionTiempoReal()
        }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(BrandLightBg)
                ) {
                    if (isLoggedIn) {
                        DashboardScreen()
                    } else {
                        LoginScreen()
                    }
                }
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandDarkBlue),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .width(450.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "Bienvenido",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    color = BrandDarkBlue,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.material3.Text(
                    text = "Inicia sesión para continuar",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00ACC1), // Tono cyan de tu subtítulo
                    modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { androidx.compose.material3.Text("Correo electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandDarkBlue,
                        focusedLabelColor = BrandDarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { androidx.compose.material3.Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandDarkBlue,
                        focusedLabelColor = BrandDarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = ""
                        lifecycleScope.launch(Dispatchers.IO) {
                            ApiService.realizarLogin(email, password) { exito, deptoId, error ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    isLoading = false
                                    if (exito) {
                                        val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
                                        sharedPref.edit().putInt("departamento_id", deptoId).apply()
                                        userDepartamentoId = deptoId
                                        isLoggedIn = true
                                        iniciarSimulacionTiempoReal()
                                    } else {
                                        errorMessage = error
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandYellow,
                        contentColor = BrandDarkBlue
                    )
                ) {
                    androidx.compose.material3.Text(
                        text = if (isLoading) "Ingresando..." else "Iniciar sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun DashboardScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Encabezado estilo "Mis Tickets" de la app móvil
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(BrandDarkBlue, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    androidx.compose.material3.Text(
                        text = "Dashboard de Tickets",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "Departamento: ${ApiService.obtenerNombreDepartamento(userDepartamentoId)}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = BrandYellow
                    )
                }

                Button(
                    onClick = { cerrarSesion() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    androidx.compose.material3.Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            }

            if (cargando) {
                androidx.compose.material3.Text(
                    text = estadoMensaje,
                    color = BrandDarkBlue,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            } else {
                if (ticketsList.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "No hay tickets pendientes para tu departamento.",
                        color = BrandDarkBlue,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                } else {
                    DashboardUI(tickets = ticketsList)
                }
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun DashboardUI(tickets: List<Ticket>) {
        val ticketsCritica = tickets.filter { it.prioridad.lowercase().contains("critica") }
        val ticketsAlta = tickets.filter { it.prioridad.lowercase() == "alta" }
        val ticketsMedia = tickets.filter { it.prioridad.lowercase() == "media" }
        val ticketsBaja = tickets.filter { it.prioridad.lowercase() == "baja" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (ticketsCritica.isNotEmpty()) {
                FilaDeTickets(titulo = "Prioridad Crítica", tickets = ticketsCritica)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (ticketsAlta.isNotEmpty()) {
                FilaDeTickets(titulo = "Prioridad Alta", tickets = ticketsAlta)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (ticketsMedia.isNotEmpty()) {
                FilaDeTickets(titulo = "Prioridad Media", tickets = ticketsMedia)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (ticketsBaja.isNotEmpty()) {
                FilaDeTickets(titulo = "Prioridad Baja", tickets = ticketsBaja)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun FilaDeTickets(titulo: String, tickets: List<Ticket>) {
        Column {
            androidx.compose.material3.Text(
                text = titulo,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = BrandDarkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(end = 32.dp)
            ) {
                items(tickets) { ticket ->
                    // Tarjetas Blancas estilo Móvil
                    Surface(
                        modifier = Modifier.width(300.dp).height(190.dp),
                        onClick = { /* Acción al presionar */ },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White,
                            contentColor = BrandDarkBlue
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val colorPrioridad = when {
                                ticket.prioridad.lowercase().contains("critica") -> Color(0xFFB71C1C)
                                ticket.prioridad.lowercase() == "alta" -> Color(0xFFFF5252)
                                ticket.prioridad.lowercase() == "media" -> BrandYellow
                                else -> Color(0xFF4CAF50)
                            }

                            androidx.compose.material3.Text(
                                text = ticket.prioridad.uppercase(),
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = if (colorPrioridad == BrandYellow) BrandDarkBlue else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(colorPrioridad, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Text(
                                text = "#${ticket.id} - ${ticket.titulo}",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandDarkBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.Text(
                                text = ticket.descripcion,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    private fun cerrarSesion() {
        val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        isLoggedIn = false
        userDepartamentoId = -1
        ticketsList.clear()
        cargando = true
        estadoMensaje = "Conectando con el servidor..."
    }

    private fun iniciarSimulacionTiempoReal() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isLoggedIn) {
                try {
                    val nuevosTickets = ApiService.obtenerTickets(userDepartamentoId)
                    withContext(Dispatchers.Main) {
                        ticketsList.clear()
                        ticketsList.addAll(nuevosTickets)
                        cargando = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        estadoMensaje = "Conectando..."
                    }
                }
                delay(5000)
            }
        }
    }
}