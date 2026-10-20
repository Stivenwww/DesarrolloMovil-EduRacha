package com.stiven.sos.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.AdminDS

/**
 * Fase 7 — identidad administrativa sobria: tablas legibles, formularios
 * claros, estados, confirmaciones y mensajes comprensibles. Sin gamificación.
 */

@Composable
fun AdminPanelHeader(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AdminDS.TextPrimary)
        Text(subtitulo, fontSize = 13.sp, color = AdminDS.TextSecondary)
    }
}

@Composable
fun AdminFormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AdminDS.Radius),
        isError = error != null,
        supportingText = { if (error != null) Text(error, color = AdminDS.Error) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AdminDS.Brand, focusedLabelColor = AdminDS.Brand
        )
    )
}

@Composable
fun EstadoChip(estado: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (estado.lowercase()) {
        "activo", "aprobada", "aceptada" -> AdminDS.Success.copy(alpha = 0.12f) to AdminDS.Success
        "pendiente", "revision", "pendiente_revision" -> AdminDS.Warning.copy(alpha = 0.15f) to AdminDS.Warning
        else -> AdminDS.Error.copy(alpha = 0.1f) to AdminDS.Error
    }
    Surface(modifier.semantics { contentDescription = "Estado $estado" }, color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(estado, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AdminConfirmDialog(
    titulo: String,
    mensaje: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AdminDS.Error)) {
                Text("Confirmar")
            }
        },
        dismissButton = { OutlinedButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

@Composable
fun AdminResumenCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(AdminDS.Radius),
        colors = CardDefaults.cardColors(containerColor = AdminDS.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminDS.Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(titulo, fontSize = 12.sp, color = AdminDS.TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AdminDS.TextPrimary)
        }
    }
}

@Composable
fun AdminEmptyState(mensaje: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(mensaje, color = AdminDS.TextSecondary, fontSize = 14.sp)
    }
}
