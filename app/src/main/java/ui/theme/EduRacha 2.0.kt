// ui/theme/EduRachaV2Theme.kt
package com.stiven.sos.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin


// ============================================================================
// COLORES V2
// ============================================================================

object EduRachaV2Colors {
  val Primary = Color(0xFF3C79F5)
  val PrimaryDark = Color(0xFF5B8FF9)
  val Success = Color(0xFF41C77C)
  val SuccessDark = Color(0xFF34A866)
  val Accent = Color(0xFFFFC864)
  val AccentDark = Color(0xFFFFB330)
  val Secondary = Color(0xFF9C6BFF)
  val SecondaryDark = Color(0xFF7E4FD9)
  val Pink = Color(0xFFFF7096)
  val PinkDark = Color(0xFFFF5277)
  val Warning = Color(0xFFFF8A3C)
  val WarningDark = Color(0xFFFF6B1C)
  val Background = Color(0xFFF7F9FC)
  val SoftGray = Color(0xFFD7DDE4)
  val White = Color(0xFFFFFFFF)
  val TextPrimary = Color(0xFF1C1C1E)
  val TextSecondary = Color(0xFF717182)
  val TextMuted = Color(0xFFA0A0AB)
}

// ============================================================================
// GRADIENTES V2
// ============================================================================

object EduRachaV2Gradients {
  val Blue = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Primary, EduRachaV2Colors.PrimaryDark)
  )
  val Green = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Success, EduRachaV2Colors.SuccessDark)
  )
  val Yellow = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Accent, EduRachaV2Colors.AccentDark)
  )
  val Purple = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Secondary, EduRachaV2Colors.SecondaryDark)
  )
  val Pink = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Pink, EduRachaV2Colors.PinkDark)
  )
  val Orange = Brush.linearGradient(
    colors = listOf(EduRachaV2Colors.Warning, EduRachaV2Colors.WarningDark)
  )
}

// ============================================================================
// CONTENEDOR V2
// ============================================================================

@Composable
fun EduRachaV2Container(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(EduRachaV2Colors.Background)
  ) {
    content()
  }
}

// ============================================================================
// HEADER V2 CON ANIMACIÓN
// ============================================================================

@Composable
fun EduRachaV2Header(
  userName: String,
  userEmail: String,
  userInitial: String,
  onNotificationClick: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
  onInfoClick: () -> Unit = {},
  hasNotificationBadge: Boolean = false,
  onBack: (() -> Unit)? = null
) {
  // Animación del avatar
  var avatarScale by remember { mutableStateOf(0f) }
  var avatarRotation by remember { mutableStateOf(-180f) }

  LaunchedEffect(Unit) {
    animate(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
      )
    ) { value, _ -> avatarScale = value }

    animate(
      initialValue = -180f,
      targetValue = 0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
      )
    ) { value, _ -> avatarRotation = value }
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color.White,
    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    shadowElevation = 2.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 20.dp)
    ) {
      if (onBack != null) {
        EduRachaV2IconButton(
          icon = Icons.Outlined.ArrowBack,
          onClick = onBack,
          modifier = Modifier.align(Alignment.CenterStart)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Avatar animado
          Box(
            modifier = Modifier
              .size(64.dp)
              .graphicsLayer {
                scaleX = avatarScale
                scaleY = avatarScale
                rotationZ = avatarRotation
              }
              .clip(CircleShape)
              .background(EduRachaV2Gradients.Blue)
              .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = userInitial,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Texto con animación de fade
          AnimatedUserInfo(userName, userEmail)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EduRachaV2IconButton(
            icon = Icons.Outlined.Notifications,
            onClick = onNotificationClick,
            hasBadge = hasNotificationBadge
          )
          EduRachaV2IconButton(
            icon = Icons.Outlined.Settings,
            onClick = onSettingsClick
          )
          EduRachaV2IconButton(
            icon = Icons.Outlined.Info,
            onClick = onInfoClick
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun AnimatedUserInfo(userName: String, userEmail: String) {
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(200)
    visible = true
  }

  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = 600)
  )

  Column(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
    Text(
      text = "Buenos días,",
      fontSize = 12.sp,
      color = EduRachaV2Colors.TextSecondary
    )
    Text(
      text = userName,
      fontSize = 18.sp,
      fontWeight = FontWeight.SemiBold,
      color = EduRachaV2Colors.TextPrimary
    )
    Text(
      text = userEmail,
      fontSize = 12.sp,
      color = EduRachaV2Colors.TextSecondary
    )
  }
}

// ============================================================================
// ICON BUTTON V2 CON ANIMACIÓN
// ============================================================================

@Composable
fun EduRachaV2IconButton(
  icon: ImageVector,
  onClick: () -> Unit,
  hasBadge: Boolean = false,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.85f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    )
  )

  Box(modifier = modifier) {
    IconButton(
      onClick = onClick,
      interactionSource = interactionSource,
      modifier = Modifier
        .size(40.dp)
        .scale(scale)
        .clip(CircleShape)
        .background(EduRachaV2Colors.Background)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = EduRachaV2Colors.TextSecondary,
        modifier = Modifier.size(20.dp)
      )
    }

    // Badge animado
    if (hasBadge) {
      AnimatedBadge()
    }
  }
}

@Composable
private fun BoxScope.AnimatedBadge() {
  val infiniteTransition = rememberInfiniteTransition()
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(
    modifier = Modifier
      .size(8.dp)
      .scale(pulseScale)
      .clip(CircleShape)
      .background(EduRachaV2Colors.Pink)
      .align(Alignment.TopEnd)
      .offset(x = (-4).dp, y = 4.dp)
  )
}

// ============================================================================
// STAT CARD V2 CON ANIMACIÓN
// ============================================================================

@Composable
fun EduRachaV2StatCard(
  icon: ImageVector,
  value: String,
  label: String,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessHigh
    )
  )

  val elevation by animateDpAsState(
    targetValue = if (isPressed) 2.dp else 4.dp,
    animationSpec = tween(100)
  )

  Card(
    modifier = modifier
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Ícono con animación de rotación sutil
      AnimatedIconBox(icon, color, label)

      Spacer(modifier = Modifier.height(12.dp))

      // Contador animado
      AnimatedCounter(value)

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = label,
        fontSize = 14.sp,
        color = EduRachaV2Colors.TextSecondary
      )
    }
  }
}

@Composable
private fun AnimatedIconBox(icon: ImageVector, color: Color, label: String) {
  var rotation by remember { mutableStateOf(0f) }

  LaunchedEffect(Unit) {
    animate(
      initialValue = -10f,
      targetValue = 10f,
      animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      )
    ) { value, _ -> rotation = value }
  }

  Box(
    modifier = Modifier
      .size(48.dp)
      .graphicsLayer { rotationZ = rotation }
      .clip(RoundedCornerShape(16.dp))
      .background(color),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = Color.White,
      modifier = Modifier.size(24.dp)
    )
  }
}

@Composable
private fun AnimatedCounter(targetValue: String) {
  var displayValue by remember { mutableStateOf(0) }
  val target = targetValue.toIntOrNull() ?: 0

  LaunchedEffect(target) {
    animate(
      initialValue = 0f,
      targetValue = target.toFloat(),
      animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    ) { value, _ ->
      displayValue = value.toInt()
    }
  }

  Text(
    text = "$displayValue",
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    color = EduRachaV2Colors.TextPrimary
  )
}

// ============================================================================
// MODULE CARD V2 CON ANIMACIÓN AVANZADA
// ============================================================================

@Composable
fun EduRachaV2ModuleCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradient: Brush,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessHigh
    )
  )

  val elevation by animateDpAsState(
    targetValue = if (isPressed) 4.dp else 8.dp,
    animationSpec = tween(100)
  )

  // Animación de las burbujas decorativas
  val infiniteTransition = rememberInfiniteTransition()
  val bubbleOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(gradient)
    ) {
      // Burbujas decorativas animadas
      Box(
        modifier = Modifier
          .size(128.dp)
          .offset(x = 200.dp, y = 80.dp)
          .graphicsLayer { translationY = bubbleOffset }
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.1f))
      )

      Box(
        modifier = Modifier
          .size(80.dp)
          .offset(x = 220.dp, y = (-10).dp)
          .graphicsLayer { translationY = -bubbleOffset }
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.05f))
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Ícono con animación de escala
        AnimatedModuleIcon(icon, title)

        Spacer(modifier = Modifier.width(8.dp))

        // Flecha con animación de traslación
        AnimatedArrow()
      }
    }
  }
}

@Composable
private fun AnimatedModuleIcon(icon: ImageVector, title: String) {
  val infiniteTransition = rememberInfiniteTransition()
  val iconScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(
    modifier = Modifier
      .size(56.dp)
      .scale(iconScale)
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = Color.White,
      modifier = Modifier.size(28.dp)
    )
  }
}

@Composable
private fun AnimatedArrow() {
  val infiniteTransition = rememberInfiniteTransition()
  val arrowOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 8f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Icon(
    imageVector = Icons.Outlined.ArrowForward,
    contentDescription = "Ir",
    tint = Color.White.copy(alpha = 0.8f),
    modifier = Modifier
      .size(24.dp)
      .graphicsLayer { translationX = arrowOffset }
  )
}

// ============================================================================
// HEADER SIMPLE V2
// ============================================================================

@Composable
fun EduRachaV2SimpleHeader(
  title: String,
  subtitle: String? = null,
  icon: ImageVector? = null,
  gradient: Brush = EduRachaV2Gradients.Blue,
  onBack: (() -> Unit)? = null
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        gradient,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
      )
      .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 64.dp)
  ) {
    if (onBack != null) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .align(Alignment.TopStart)
          .size(40.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.2f))
      ) {
        Icon(
          imageVector = Icons.Outlined.ArrowBack,
          contentDescription = "Volver",
          tint = Color.White
        )
      }
    }

    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(48.dp),
          tint = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
      )

      if (subtitle != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          fontSize = 14.sp,
          color = Color.White.copy(alpha = 0.9f)
        )
      }
    }
  }
}

// ============================================================================
// BOTÓN V2 CON ANIMACIÓN
// ============================================================================

@Composable
fun EduRachaV2Button(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: EduRachaV2ButtonVariant = EduRachaV2ButtonVariant.Primary,
  gradient: Brush? = null,
  icon: ImageVector? = null,
  enabled: Boolean = true
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
  )

  val backgroundColor = when (variant) {
    EduRachaV2ButtonVariant.Primary -> gradient ?: EduRachaV2Gradients.Blue
    EduRachaV2ButtonVariant.Secondary -> Brush.linearGradient(
      listOf(EduRachaV2Colors.Success, EduRachaV2Colors.Success)
    )
    EduRachaV2ButtonVariant.Outline -> Brush.linearGradient(
      listOf(Color.White, Color.White)
    )
    EduRachaV2ButtonVariant.Ghost -> Brush.linearGradient(
      listOf(Color.Transparent, Color.Transparent)
    )
  }

  val contentColor = when (variant) {
    EduRachaV2ButtonVariant.Outline -> EduRachaV2Colors.TextPrimary
    EduRachaV2ButtonVariant.Ghost -> EduRachaV2Colors.Primary
    else -> Color.White
  }

  Button(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .scale(scale),
    enabled = enabled,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = contentColor
    ),
    elevation = ButtonDefaults.buttonElevation(
      defaultElevation = if (variant == EduRachaV2ButtonVariant.Ghost) 0.dp else 2.dp
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor, shape = RoundedCornerShape(16.dp))
        .then(
          if (variant == EduRachaV2ButtonVariant.Outline)
            Modifier.border(2.dp, EduRachaV2Colors.SoftGray, RoundedCornerShape(16.dp))
          else Modifier
        ),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = contentColor
        )
      }
    }
  }
}

enum class EduRachaV2ButtonVariant {
  Primary, Secondary, Outline, Ghost
}

// ============================================================================
// OTROS COMPONENTES BÁSICOS
// ============================================================================

@Composable
fun EduRachaV2Badge(
  text: String,
  color: Color = EduRachaV2Colors.Primary
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = color.copy(alpha = 0.2f)
  ) {
    Text(
      text = text,
      color = color,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

@Composable
fun EduRachaV2ProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  color: Color = EduRachaV2Colors.Primary,
  showLabel: Boolean = false
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(RoundedCornerShape(50))
        .background(EduRachaV2Colors.Background)
    ) {
      val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing)
      )

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(animatedProgress)
          .clip(RoundedCornerShape(50))
          .background(color)
      )
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "${(progress * 100).toInt()}% completado",
        fontSize = 12.sp,
        color = EduRachaV2Colors.TextSecondary
      )
    }
  }
}

// ============================================================================
// COMPONENTES PARA CURSOS INSCRITOS
// ============================================================================

@Composable
fun EduRachaV2StatCardCompact(
  icon: ImageVector,
  valor: String,
  label: String,
  color: Color,
  backgroundColor: Color = Color.White,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = backgroundColor,
    shadowElevation = 4.dp,
    onClick = onClick
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            listOf(
              color.copy(alpha = 0.08f),
              backgroundColor
            )
          )
        )
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(24.dp)
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          valor,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = color
        )
        Text(
          label,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = EduRachaV2Colors.TextSecondary
        )
      }
    }
  }
}

@Composable
fun EduRachaV2VidasCard(
  vidasActuales: Int,
  vidasMax: Int,
  minutosParaProxima: Int,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = Color.White,
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(EduRachaV2Colors.Pink.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          val infiniteTransition = rememberInfiniteTransition()
          val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (vidasActuales > 0) 1.1f else 1f,
            animationSpec = infiniteRepeatable(
              animation = tween(800),
              repeatMode = RepeatMode.Reverse
            )
          )

          Icon(
            Icons.Outlined.Favorite,
            contentDescription = null,
            tint = EduRachaV2Colors.Pink,
            modifier = Modifier
              .size(28.dp)
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
              }
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "ENERGÍA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaV2Colors.TextSecondary,
            letterSpacing = 0.5.sp
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              "$vidasActuales",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = EduRachaV2Colors.Pink
            )
            Text(
              "/ $vidasMax",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = EduRachaV2Colors.TextSecondary,
              modifier = Modifier.padding(bottom = 2.dp)
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(vidasMax) { index ->
              Box(
                modifier = Modifier
                  .width(12.dp)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(
                    if (index < vidasActuales) EduRachaV2Colors.Pink
                    else Color(0xFFE0E0E0)
                  )
              )
            }
          }
        }
      }

      if (vidasActuales < vidasMax && minutosParaProxima > 0) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = EduRachaV2Colors.Primary.copy(alpha = 0.1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Outlined.Schedule,
              contentDescription = null,
              tint = EduRachaV2Colors.Primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              "${minutosParaProxima}m",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = EduRachaV2Colors.Primary
            )
          }
        }
      }
    }
  }
}

// ============================================================================
// ESTADOS: LOADING, ERROR, EMPTY
// ============================================================================

@Composable
fun EduRachaV2LoadingState(
  message: String = "Cargando...",
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 60.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(50.dp),
        color = EduRachaV2Colors.Success,
        strokeWidth = 4.dp
      )

      Text(
        text = message,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = EduRachaV2Colors.TextSecondary
      )
    }
  }
}

@Composable
fun EduRachaV2ErrorState(
  title: String = "Algo salió mal",
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 60.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      Box(
        modifier = Modifier
          .size(100.dp)
          .clip(CircleShape)
          .background(EduRachaV2Colors.Pink.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Outlined.ErrorOutline,
          contentDescription = null,
          modifier = Modifier.size(48.dp),
          tint = EduRachaV2Colors.Pink
        )
      }

      Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = EduRachaV2Colors.TextPrimary
      )

      Text(
        text = message,
        fontSize = 14.sp,
        color = EduRachaV2Colors.TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      EduRachaV2Button(
        text = "Reintentar",
        onClick = onRetry,
        icon = Icons.Outlined.Refresh,
        gradient = EduRachaV2Gradients.Pink,
        modifier = Modifier.fillMaxWidth(0.8f)
      )
    }
  }
}

@Composable
fun EduRachaV2EmptyState(
  icon: ImageVector,
  iconColor: Color,
  title: String,
  message: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 60.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      Box(
        modifier = Modifier
          .size(120.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon,
          contentDescription = null,
          modifier = Modifier.size(60.dp),
          tint = iconColor
        )
      }

      Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = EduRachaV2Colors.TextPrimary,
        textAlign = TextAlign.Center
      )

      Text(
        text = message,
        fontSize = 14.sp,
        color = EduRachaV2Colors.TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )
    }
  }
}

// ============================================================================
// ANIMACIÓN DE BURBUJAS DECORATIVAS
// ============================================================================

@Composable
fun AnimatedBubblesDecoration(
  modifier: Modifier = Modifier,
  color: Color = Color.White
) {
  val infiniteTransition = rememberInfiniteTransition()

  val bubble1Offset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 10f,
    animationSpec = infiniteRepeatable(
      animation = tween(2500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  val bubble2Rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(20000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )

  Box(modifier = modifier) {
    // Burbuja grande (reducida)
    Box(
      modifier = Modifier
        .size(60.dp)
        .offset(x = 200.dp, y = 10.dp)
        .graphicsLayer {
          translationY = bubble1Offset
          rotationZ = bubble2Rotation
        }
        .clip(CircleShape)
        .background(color.copy(alpha = 0.08f))
    )

    // Burbuja mediana (reducida)
    Box(
      modifier = Modifier
        .size(45.dp)
        .offset(x = 150.dp, y = 40.dp)
        .graphicsLayer {
          translationY = -bubble1Offset * 0.8f
          rotationZ = -bubble2Rotation * 0.5f
        }
        .clip(RoundedCornerShape(14.dp))
        .background(color.copy(alpha = 0.06f))
    )

    // Burbuja pequeña (reducida)
    Box(
      modifier = Modifier
        .size(35.dp)
        .offset(x = 20.dp, y = 15.dp)
        .graphicsLayer {
          translationY = bubble1Offset * 0.7f
          rotationZ = bubble2Rotation * 0.3f
        }
        .clip(CircleShape)
        .background(color.copy(alpha = 0.05f))
    )
  }
}

// ============================================================================
// COMPONENTES AVANZADOS PARA RESULTADO QUIZ
// ============================================================================

enum class TipoMedalla(
  val emoji: String,
  val colorMedalla: Color,
  val colorCinta: Color
) {
  ORO("🏆", Color(0xFFFFD700), Color(0xFFFFA500)),
  PLATA("⭐", Color(0xFFC0C0C0), Color(0xFF9370DB)),
  BRONCE("🎖️", Color(0xFFCD7F32), Color(0xFFFF6347))
}

@Composable
fun MedallaAnimada(
  tipo: TipoMedalla,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "medalla")

  val rotation by infiniteTransition.animateFloat(
    initialValue = -10f,
    targetValue = 10f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "rotation"
  )

  val scale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Box(
    modifier = modifier
      .size(120.dp)
      .graphicsLayer {
        rotationZ = rotation
        scaleX = scale
        scaleY = scale
      },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val path = Path().apply {
        moveTo(size.width * 0.3f, 0f)
        lineTo(size.width * 0.7f, 0f)
        lineTo(size.width * 0.6f, size.height * 0.4f)
        lineTo(size.width * 0.5f, size.height * 0.35f)
        lineTo(size.width * 0.4f, size.height * 0.4f)
        close()
      }

      drawPath(
        path = path,
        color = tipo.colorCinta,
        alpha = 0.9f
      )
    }

    Box(
      modifier = Modifier
        .size(80.dp)
        .offset(y = 20.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(
              tipo.colorMedalla,
              tipo.colorMedalla.copy(alpha = 0.7f)
            )
          )
        )
        .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = tipo.emoji,
        fontSize = 36.sp
      )
    }
  }
}

data class TimelineItem(
  val titulo: String,
  val descripcion: String,
  val completado: Boolean
)