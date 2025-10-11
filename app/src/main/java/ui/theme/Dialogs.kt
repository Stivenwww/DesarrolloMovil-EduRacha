
package com.stiven.desarrollomovil.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.stiven.desarrollomovil.ui.theme.CustomShapes
import com.stiven.desarrollomovil.ui.theme.EduRachaColors

@Composable
fun EduRachaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Parámetro para sobreescribir el color por defecto
    backgroundColor: Color = EduRachaColors.Primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CustomShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EduRachaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CustomShapes.Button,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EduRachaColors.Primary
        ),
        border = BorderStroke(ButtonDefaults.outlinedButtonBorder.width, EduRachaColors.Primary)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EduRachaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CustomShapes.Button,
        colors = ButtonDefaults.textButtonColors(
            contentColor = EduRachaColors.TextSecondary
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}
