package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary

fun getPlantTypeIcon(speciesName: String): ImageVector {
    return when {
        speciesName.contains("해바라기") || speciesName.contains("Sunflower", ignoreCase = true) -> Icons.Filled.LocalFlorist
        speciesName.contains("몬스테라") || speciesName.contains("Monstera", ignoreCase = true) -> Icons.Filled.Park
        speciesName.contains("아이비") || speciesName.contains("Ivy", ignoreCase = true) ||
            speciesName.contains("포토스") || speciesName.contains("Pothos", ignoreCase = true) -> Icons.Filled.Grass
        speciesName.contains("다육") || speciesName.contains("선인장") ||
            speciesName.contains("스투키") || speciesName.contains("Snake", ignoreCase = true) -> Icons.Filled.Spa
        speciesName.contains("고무") || speciesName.contains("Rubber", ignoreCase = true) -> Icons.Filled.FilterVintage
        else -> Icons.Filled.Eco
    }
}

fun getPlantTypeIconColor(speciesName: String): Color {
    return when {
        speciesName.contains("해바라기") || speciesName.contains("Sunflower", ignoreCase = true) -> GoldAccent
        speciesName.contains("몬스테라") || speciesName.contains("Monstera", ignoreCase = true) -> GreenPrimary
        speciesName.contains("아이비") || speciesName.contains("Ivy", ignoreCase = true) ||
            speciesName.contains("포토스") || speciesName.contains("Pothos", ignoreCase = true) -> GreenSecondary
        speciesName.contains("다육") || speciesName.contains("선인장") ||
            speciesName.contains("스투키") || speciesName.contains("Snake", ignoreCase = true) -> Color(0xFF52796F)
        speciesName.contains("고무") || speciesName.contains("Rubber", ignoreCase = true) -> Color(0xFF8A6F3D)
        else -> GreenPrimary
    }
}
