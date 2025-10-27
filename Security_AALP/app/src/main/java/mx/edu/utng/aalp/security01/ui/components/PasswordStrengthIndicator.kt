package mx.edu.utng.aalp.security01.ui.components


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Indicador visual de fortaleza de contraseña
 *
 * CRITERIOS:
 * - Longitud >= 8: +25 puntos
 * - Tiene mayúsculas: +25 puntos
 * - Tiene números: +25 puntos
 * - Tiene caracteres especiales: +25 puntos
 */
@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = calculatePasswordStrength(password)
    val progress by animateFloatAsState(targetValue = strength / 100f, label = "progress")

    val (color, label) = when {
        strength < 25 -> Color.Red to "Muy débil"
        strength < 50 -> Color(0xFFFF9800) to "Débil"
        strength < 75 -> Color(0xFFFFEB3B) to "Media"
        else -> Color.Green to "Fuerte"
    }

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fortaleza: $label",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

/**
 * Calcula la fortaleza de una contraseña
 * @return Int entre 0 y 100
 */
private fun calculatePasswordStrength(password: String): Int {
    if (password.isEmpty()) return 0

    var strength = 0

    // Longitud
    if (password.length >= 8) strength += 25

    // Mayúsculas
    if (password.any { it.isUpperCase() }) strength += 25

    // Números
    if (password.any { it.isDigit() }) strength += 25

    // Caracteres especiales
    val specialChars = "!@#\$%^&*()_+-=[]{}|;:'\",.<>?/"
    if (password.any { it in specialChars }) strength += 25

    return strength
}
