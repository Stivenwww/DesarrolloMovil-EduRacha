
package com.stiven.desarrollomovil.ui.theme.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.*

// ============================================
// TEXTFIELD ESTÁNDAR PREMIUM
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = EduRachaColors.TextHint
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border,
                focusedLabelColor = EduRachaColors.Primary,
                unfocusedLabelColor = EduRachaColors.TextSecondary,
                cursorColor = EduRachaColors.Primary,
                errorBorderColor = EduRachaColors.Error,
                errorLabelColor = EduRachaColors.Error,
                disabledBorderColor = EduRachaColors.BorderLight,
                focusedContainerColor = EduRachaColors.Surface,
                unfocusedContainerColor = EduRachaColors.Surface
            ),
            shape = CustomShapes.TextField,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        AnimatedVisibility(
            visible = isError && errorMessage.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.padding(start = Spacing.medium, top = Spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                Text(
                    text = errorMessage,
                    color = EduRachaColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================
// TEXTFIELD DE CONTRASEÑA
// ============================================
@Composable
fun EduRachaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    EduRachaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = if (enabled) EduRachaColors.Primary else EduRachaColors.TextDisabled
                )
            }
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = modifier
    )
}

// ============================================
// TEXTFIELD DE BÚSQUEDA
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    onSearchClick: () -> Unit = {},
    onClearClick: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = EduRachaColors.TextHint
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = EduRachaColors.Primary
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = {
                    onClearClick()
                    onValueChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EduRachaColors.Primary,
            unfocusedBorderColor = EduRachaColors.Border,
            cursorColor = EduRachaColors.Primary,
            focusedContainerColor = EduRachaColors.Surface,
            unfocusedContainerColor = EduRachaColors.Surface
        ),
        shape = CustomShapes.TextField,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

// ============================================
// TEXTFIELD MULTILINEA (Para descripciones)
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLines: Int = 5,
    minLines: Int = 3,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String = "",
    maxCharacters: Int? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (maxCharacters == null || newValue.length <= maxCharacters) {
                    onValueChange(newValue)
                }
            },
            label = {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = EduRachaColors.TextHint
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = maxLines,
            enabled = enabled,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border,
                focusedLabelColor = EduRachaColors.Primary,
                unfocusedLabelColor = EduRachaColors.TextSecondary,
                cursorColor = EduRachaColors.Primary,
                errorBorderColor = EduRachaColors.Error,
                errorLabelColor = EduRachaColors.Error
            ),
            shape = CustomShapes.TextField,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.extraSmall, start = Spacing.medium, end = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mensaje de error
            if (isError && errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = EduRachaColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Contador de caracteres
            if (maxCharacters != null) {
                Text(
                    text = "${value.length}/$maxCharacters",
                    color = if (value.length >= maxCharacters) EduRachaColors.Error else EduRachaColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// ============================================
// TEXTFIELD CON CONTADOR
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaTextFieldWithCounter(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxCharacters: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        EduRachaTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= maxCharacters) {
                    onValueChange(newValue)
                }
            },
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            isError = isError,
            errorMessage = errorMessage,
            enabled = enabled
        )

        Text(
            text = "${value.length}/$maxCharacters",
            color = if (value.length >= maxCharacters) EduRachaColors.Warning else EduRachaColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = Spacing.extraSmall, end = Spacing.medium)
        )
    }
}