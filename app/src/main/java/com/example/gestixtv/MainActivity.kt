package com.example.gestixtv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Paleta oficial de Gestix (igual que la web: Login.css, Header.css, TicketList.css)
val BrandNavy = Color(0xFF1B3A5C)
val BrandLoginBg = Color(0xFF1B2437)
val BrandYellow = Color(0xFFFFD100)
val BrandTeal = Color(0xFF2DC4D4)
val BrandPageBg = Color(0xFFF5F5F5)
val BrandTextGray = Color(0xFF2D3748)
val BrandCyanStart = Color(0xFF00D4FF)
val BrandCyanEnd = Color(0xFF0099FF)

// Badges pastel, idénticos a los de la tabla de tickets de la web
val BadgeRedBg = Color(0xFFFDE8E8); val BadgeRedText = Color(0xFFC0392B)
val BadgeOrangeBg = Color(0xFFFEF0E6); val BadgeOrangeText = Color(0xFFE67E22)
val BadgeBlueBg = Color(0xFFE8F4FD); val BadgeBlueText = Color(0xFF2980B9)
val BadgeGreenBg = Color(0xFFE8F8F0); val BadgeGreenText = Color(0xFF27AE60)
val BadgeGrayBg = Color(0xFFF0F0F0); val BadgeGrayText = Color(0xFF666666)

private const val FALLBACK_REFRESH_MS = 60_000L

class MainActivity : ComponentActivity() {

    private val ticketsList = mutableStateListOf<Ticket>()
    var estadoMensaje by mutableStateOf("Conectando con el servidor...")
    var cargando by mutableStateOf(true)

    var isLoggedIn by mutableStateOf(false)
    var userDepartamentoId by mutableStateOf(-1)
    var departamentoNombre by mutableStateOf("")
    var enVivo by mutableStateOf(false)
    private var authToken: String? = null

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
        val savedDepto = sharedPref.getInt("departamento_id", -1)
        val savedToken = sharedPref.getString("access_token", null)

        if (savedDepto != -1) {
            userDepartamentoId = savedDepto
            authToken = savedToken
            isLoggedIn = true
            iniciarTiempoReal()
        }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(BrandPageBg)
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
                .background(BrandLoginBg)
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tarjeta de login, igual que login-card de la web
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .shadow(30.dp, RoundedCornerShape(18.dp), clip = false)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(horizontal = 45.dp, vertical = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Text(
                        text = "Gestix",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(BrandCyanStart, BrandCyanEnd)),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    androidx.compose.material3.Text(
                        text = "SISTEMA DE GESTIÓN DE TICKETS",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Color(0xFF999999),
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { androidx.compose.material3.Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF999999)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandTeal,
                            focusedLabelColor = BrandNavy,
                            unfocusedContainerColor = Color(0xFFF8F9FA),
                            focusedContainerColor = Color(0xFFF0F9FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { androidx.compose.material3.Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF999999)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandTeal,
                            focusedLabelColor = BrandNavy,
                            unfocusedContainerColor = Color(0xFFF8F9FA),
                            focusedContainerColor = Color(0xFFF0F9FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage.isNotEmpty()) {
                        androidx.compose.material3.Text(
                            text = errorMessage,
                            color = BadgeRedText,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BadgeRedBg, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Botón con degradado amarillo, igual que .submit-btn de la web
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(BrandYellow, Color(0xFFFFED4E))))
                            .clickable(enabled = !isLoading) {
                                isLoading = true
                                errorMessage = ""
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val result = ApiService.realizarLogin(email, password)
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        if (result.success) {
                                            val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
                                            sharedPref.edit()
                                                .putInt("departamento_id", result.departamentoId)
                                                .putString("access_token", result.token)
                                                .apply()
                                            userDepartamentoId = result.departamentoId
                                            authToken = result.token
                                            isLoggedIn = true
                                            iniciarTiempoReal()
                                        } else {
                                            errorMessage = result.error
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = if (isLoading) "Ingresando..." else "Iniciar sesión",
                            color = BrandNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Tarjetas informativas, igual que .info-cards de la web
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.width(360.dp)
                ) {
                    InfoCard(Icons.Default.CheckCircle, "Fácil de usar", "Interfaz intuitiva para gestionar tus tickets")
                    InfoCard(Icons.Default.Lock, "Seguro", "Tus datos están protegidos con encriptación")
                    InfoCard(Icons.Default.PlayArrow, "Rápido", "Respuestas inmediatas a tus solicitudes")
                }
            }
        }
    }

    @Composable
    fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrandTeal, RoundedCornerShape(16.dp))
                .background(Color(0xFF111C27), RoundedCornerShape(16.dp))
                .padding(vertical = 26.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(14.dp))
            androidx.compose.material3.Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.material3.Text(
                text = description,
                color = Color(0xFF9FB3C8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun LiveIndicator() {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
            label = "pulseAlpha"
        )
        val dotColor = if (enVivo) BrandTeal else BadgeRedText

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (enVivo) pulseAlpha else 1f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text(
                text = if (enVivo) "En vivo" else "Reconectando…",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold
            )
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun DashboardScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior estilo .app-header de la web: navy plano, sin degradado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .background(BrandNavy)
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    androidx.compose.material3.Text(
                        text = "Gestix · Dashboard de Tickets",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "Departamento: ${departamentoNombre.ifBlank { "Cargando..." }}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = BrandYellow,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LiveIndicator()
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { cerrarSesion() }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    androidx.compose.material3.Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (cargando) {
                androidx.compose.material3.Text(
                    text = estadoMensaje,
                    color = BrandNavy,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                if (ticketsList.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "No hay tickets pendientes para tu departamento.",
                        color = BrandNavy,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(32.dp)
                    )
                } else {
                    DashboardUI(tickets = ticketsList)
                }
            }
        }
    }

    // Sin scroll manual (no tiene sentido con un control remoto): en vez de
    // apachurrar todos los tickets en la pantalla —lo que los volvía
    // ilegibles cuando había muchos—, se reparten en páginas de tamaño fijo
    // que rotan solas cada pocos segundos, ordenados por prioridad.
    private fun prioridadRango(prioridad: String) = when (prioridad.lowercase()) {
        "critica", "crítica" -> 0
        "alta" -> 1
        "media" -> 2
        else -> 3
    }

    companion object {
        private const val COLUMNAS = 3
        private const val FILAS_POR_PAGINA = 4
        private const val TICKETS_POR_PAGINA = COLUMNAS * FILAS_POR_PAGINA
        private const val ROTACION_PAGINA_MS = 8_000L
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun DashboardUI(tickets: List<Ticket>) {
        val ordenados = tickets.sortedBy { prioridadRango(it.prioridad) }
        val paginas = remember(ordenados) { ordenados.chunked(TICKETS_POR_PAGINA) }
        var paginaActual by remember { mutableStateOf(0) }

        // Si la lista cambia (nuevo ticket, resuelto, etc.) y la página actual
        // ya no existe, regresamos a la primera en vez de mostrar una vacía.
        LaunchedEffect(paginas.size) {
            if (paginaActual >= paginas.size) paginaActual = 0
        }

        // Auto-rotación: solo si hay más de una página que mostrar.
        LaunchedEffect(paginas.size) {
            if (paginas.size <= 1) return@LaunchedEffect
            while (isActive) {
                delay(ROTACION_PAGINA_MS)
                paginaActual = (paginaActual + 1) % paginas.size
            }
        }

        val ticketsPagina = paginas.getOrElse(paginaActual) { emptyList() }
        val porColumna = List(COLUMNAS) { col -> ticketsPagina.filterIndexed { i, _ -> i % COLUMNAS == col } }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                porColumna.forEach { ticketsColumna ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        repeat(FILAS_POR_PAGINA) { fila ->
                            val ticket = ticketsColumna.getOrNull(fila)
                            if (ticket != null) {
                                TicketRow(ticket = ticket, modifier = Modifier.weight(1f).fillMaxWidth())
                            } else {
                                Spacer(modifier = Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }

            if (paginas.size > 1) {
                PaginaIndicador(total = paginas.size, actual = paginaActual)
            }
        }
    }

    @Composable
    fun PaginaIndicador(total: Int, actual: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(if (i == actual) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i == actual) BrandTeal else Color(0xFFCBD5E0))
                )
            }
        }
    }

    // Urgencia real de un ticket: critica pulsa (necesita atención YA),
    // alta es roja fija, media naranja, baja/sin definir azul — mismo mapeo
    // de colores que las badges de prioridad de la web (TicketList.css).
    private fun urgenciaColor(prioridad: String): Color = when (prioridad.lowercase()) {
        "critica", "crítica" -> BadgeRedText
        "alta" -> BadgeRedText
        "media" -> BadgeOrangeText
        else -> BadgeBlueText
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun TicketRow(ticket: Ticket, modifier: Modifier = Modifier) {
        // Mismos colores de badge que la tabla de tickets de la web (TicketList.css)
        val esCritica = ticket.prioridad.lowercase().contains("critica") || ticket.prioridad.lowercase().contains("crítica")
        val (prioridadBg, prioridadText) = when {
            esCritica || ticket.prioridad.lowercase() == "alta" -> BadgeRedBg to BadgeRedText
            ticket.prioridad.lowercase() == "media" -> BadgeOrangeBg to BadgeOrangeText
            else -> BadgeBlueBg to BadgeBlueText
        }
        val (estadoBg, estadoText) = when (ticket.estado.lowercase()) {
            "abierto" -> BadgeGreenBg to BadgeGreenText
            "pendiente" -> BadgeBlueBg to BadgeBlueText
            else -> BadgeGrayBg to BadgeGrayText
        }
        val colorUrgencia = urgenciaColor(ticket.prioridad)

        // Las tarjetas críticas laten sutilmente para saltar a la vista desde
        // el otro lado de la sala, sin depender de leer el texto de la badge.
        val infiniteTransition = rememberInfiniteTransition(label = "urgencia")
        val pulso by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
            label = "pulso"
        )

        Surface(
            modifier = modifier.shadow(2.dp, RoundedCornerShape(10.dp), clip = false),
            onClick = { /* Acción al presionar */ },
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (esCritica) Color(0xFFFFF3F3) else Color.White,
                contentColor = BrandNavy,
                focusedContainerColor = Color(0xFFF8FAFC),
                focusedContentColor = BrandNavy
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, BrandTeal),
                    shape = RoundedCornerShape(10.dp)
                )
            )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Barra de acento: el color de urgencia se ve de un vistazo
                // sin tener que leer la badge.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(colorUrgencia.copy(alpha = if (esCritica) pulso else 1f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "#${ticket.id}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = ticket.titulo,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = ticket.descripcion,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(text = ticket.prioridad, background = prioridadBg, textColor = prioridadText)
                    Badge(text = ticket.estado, background = estadoBg, textColor = estadoText)
                }
                }
            }
        }
    }

    @Composable
    fun Badge(text: String, background: Color, textColor: Color) {
        androidx.compose.material3.Text(
            text = text.replaceFirstChar { it.uppercase() },
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(background)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }

    private fun cerrarSesion() {
        val sharedPref = getSharedPreferences("GestixSession", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        TicketSocketManager.disconnect()
        isLoggedIn = false
        userDepartamentoId = -1
        authToken = null
        departamentoNombre = ""
        enVivo = false
        ticketsList.clear()
        cargando = true
        estadoMensaje = "Conectando con el servidor..."
    }

    private fun refrescarTickets() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nuevosTickets = ApiService.obtenerTickets(authToken, userDepartamentoId)
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
        }
    }

    private fun iniciarTiempoReal() {
        // Nombre real del departamento (antes era un mapa fijo en el código)
        lifecycleScope.launch(Dispatchers.IO) {
            val departamentos = ApiService.obtenerDepartamentos()
            val nombre = departamentos.find { it.id == userDepartamentoId }?.nombre ?: "General / Todos"
            withContext(Dispatchers.Main) { departamentoNombre = nombre }
        }

        // Carga inicial
        refrescarTickets()

        // Tiempo real vía websocket: el backend avisa cuando se crea, asigna o
        // resuelve un ticket y refrescamos la lista al instante.
        TicketSocketManager.connect(
            onTicketChanged = { lifecycleScope.launch(Dispatchers.Main) { refrescarTickets() } },
            onConnectionChange = { conectado -> lifecycleScope.launch(Dispatchers.Main) { enVivo = conectado } }
        )

        // Respaldo por si el socket se desconecta silenciosamente
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isLoggedIn) {
                delay(FALLBACK_REFRESH_MS)
                refrescarTickets()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            TicketSocketManager.disconnect()
        }
    }
}
