package com.elysium.guild.ui.screens

import android.os.Parcelable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elysium.guild.ui.components.DynamicElysiumBackground
import com.elysium.guild.ui.components.ElysiumGlassCard
import com.elysium.guild.ui.theme.ElysiumGold
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.util.Locale

enum class RelicType(val displayName: String, val icon: ImageVector, val darkColor: Color, val lightColor: Color) {
    ORIGIN_OF_DESTRUCTION("Origin of Destruction", Icons.Default.Whatshot, Color(0xFFEF5350), Color(0xFFC62828)),
    BARRIER_PROTECTION("Barrier Protection", Icons.Default.Shield, Color(0xFF42A5F5), Color(0xFF1565C0)),
    CRYSTAL_OF_LIFE("Crystal of Life", Icons.Default.Favorite, Color(0xFF66BB6A), Color(0xFF2E7D32)),
    MAGIC_STORM("Magic Storm", Icons.Default.Thunderstorm, Color(0xFFFFD600), Color(0xFFF9A825))
}

@Stable
@Parcelize
data class RelicData(
    val type: RelicType,
    val currentLevel: String,
    val targetLevel: String,
    val isEnabled: Boolean = true
) : Parcelable

@Composable
fun RelicScreen() {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val standardCosts = remember {
        listOf(
            15, 25, 36, 48, 62, 78, 96, 116, 138, 163, 192, 225, 263, 306, 355, 410, 471, 539, 614, 697,
            790, 890, 990, 1090, 1190, 1340, 1490, 1760, 1964, 2172, 2400, 2650, 2924, 3224, 3552, 3910,
            4300, 4724, 5184, 5682, 6220, 6800, 7424, 8094, 8812, 9580, 10400, 11274, 12204, 13192, 14240,
            15350, 16524, 17764, 19072, 20450, 21900, 23424, 25024, 26702, 28460, 30300, 32224, 34234,
            36332, 38520, 40800, 43174, 45644, 48212, 50880, 53650, 56524, 59504, 62592, 65790, 69100,
            72524, 76064, 79722, 83500, 87400, 91424, 95574, 99852, 104260, 108800, 113474, 118284, 123232,
            128320, 133550, 138924, 144444, 150112, 155930, 161900, 168024, 174304
        )
    }

    val magicStormCosts = remember {
        listOf(
            4999, 5104, 5220, 5347, 5486, 5636, 5799, 5977, 6170, 6381, 6611, 6863, 7138, 7438, 7765, 8122,
            8511, 8934, 9393, 9891, 10428, 11008, 11632, 12303, 13022, 13791, 14611, 15486, 16417, 17405,
            18453, 19562, 20734, 21972, 23278, 24654, 26102, 27626, 29228, 30912, 32681, 34540, 36492,
            38542, 40697, 42961, 45341, 47845, 50481, 53256, 56181, 59267, 62525, 65967, 69607, 73460,
            77543, 81873, 86468, 91351, 96542, 102066, 107949, 114219, 120904, 128037, 135652, 143786,
            152476, 161765, 171695, 182315, 193673, 205822, 218818, 232719, 247587, 263489, 280494, 298675,
            318108, 338876, 361062, 384757, 410054, 437052, 465855, 496569, 529310, 564194, 601346, 640896,
            682977, 727732, 775307, 825855, 879535, 936514, 996965
        )
    }

    var relic1 by rememberSaveable { mutableStateOf(RelicData(RelicType.ORIGIN_OF_DESTRUCTION, "1", "100")) }
    var relic2 by rememberSaveable { mutableStateOf(RelicData(RelicType.BARRIER_PROTECTION, "1", "100")) }
    var relic3 by rememberSaveable { mutableStateOf(RelicData(RelicType.CRYSTAL_OF_LIFE, "1", "100")) }
    var relic4 by rememberSaveable { mutableStateOf(RelicData(RelicType.MAGIC_STORM, "1", "100")) }

    val totalOverallCost = remember(relic1, relic2, relic3, relic4) {
        listOf(relic1, relic2, relic3, relic4).sumOf { data ->
            if (!data.isEnabled) 0L else {
                val start = data.currentLevel.toIntOrNull() ?: 1
                val end = data.targetLevel.toIntOrNull() ?: 1
                val costs = if (data.type == RelicType.MAGIC_STORM) magicStormCosts else standardCosts
                
                var sum = 0L
                if (start < end) {
                    for (i in (start - 1) until (end - 1)) {
                        if (i < costs.size) sum += costs[i]
                    }
                }
                sum
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground(scrollOffset = scrollState.value.toFloat()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RELIC CALCULATOR",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            letterSpacing = 2.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = ElysiumGold.copy(alpha = 0.5f),
                                blurRadius = 15f
                            )
                        ),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MULTI-RELIC ESTIMATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overall Total Card
                val goldColor = if (isDark) ElysiumGold else Color(0xFFB8860B)
                
                ElysiumGlassCard(
                    statusColor = goldColor,
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = goldColor.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL TEMPORAL PC REQUIRED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, null, tint = goldColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = NumberFormat.getNumberInstance(Locale.US).format(totalOverallCost),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = goldColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Individual Relic Inputs
                RelicInputRow(data = relic1, onDataChange = { relic1 = it }, costs = standardCosts, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
                RelicInputRow(data = relic2, onDataChange = { relic2 = it }, costs = standardCosts, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
                RelicInputRow(data = relic3, onDataChange = { relic3 = it }, costs = standardCosts, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
                RelicInputRow(data = relic4, onDataChange = { relic4 = it }, costs = magicStormCosts, isDark = isDark)

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun RelicInputRow(
    data: RelicData,
    onDataChange: (RelicData) -> Unit,
    costs: List<Int>,
    isDark: Boolean
) {
    val relicColor = if (isDark) data.type.darkColor else data.type.lightColor
    
    val individualCost = remember(data.currentLevel, data.targetLevel, data.isEnabled) {
        if (!data.isEnabled) 0L else {
            val start = data.currentLevel.toIntOrNull() ?: 1
            val end = data.targetLevel.toIntOrNull() ?: 1
            var sum = 0L
            if (start < end) {
                for (i in (start - 1) until (end - 1)) {
                    if (i < costs.size) sum += costs[i]
                }
            }
            sum
        }
    }

    ElysiumGlassCard(
        statusColor = if (data.isEnabled) relicColor else Color.Gray.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .alpha(if (data.isEnabled) 1f else 0.6f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = data.isEnabled,
                    onCheckedChange = { onDataChange(data.copy(isEnabled = it)) },
                    colors = CheckboxDefaults.colors(checkedColor = relicColor)
                )
                Icon(data.type.icon, null, tint = if (data.isEnabled) relicColor else Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = data.type.displayName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (data.isEnabled) relicColor else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                if (data.isEnabled && individualCost > 0) {
                    Text(
                        text = "+${NumberFormat.getNumberInstance(Locale.US).format(individualCost)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = relicColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = data.currentLevel,
                    enabled = data.isEnabled,
                    onValueChange = { input ->
                        val numeric = input.filter { it.isDigit() }
                        if (numeric.isEmpty()) {
                            onDataChange(data.copy(currentLevel = ""))
                        } else {
                            var value = numeric.toIntOrNull() ?: 1
                            if (value < 1) value = 1
                            if (value > 100) value = 100
                            
                            var newTarget = data.targetLevel.toIntOrNull() ?: 1
                            if (value in newTarget..<100) newTarget = value + 1
                            else if (value >= 100) newTarget = 100

                            onDataChange(data.copy(
                                currentLevel = value.toString(),
                                targetLevel = newTarget.toString()
                            ))
                        }
                    },
                    label = { Text("Current", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    prefix = { Text("Lv.", fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = relicColor,
                        unfocusedBorderColor = relicColor.copy(alpha = 0.5f),
                        focusedLabelColor = relicColor,
                        unfocusedLabelColor = relicColor.copy(alpha = 0.7f),
                        cursorColor = relicColor,
                        focusedPrefixColor = relicColor,
                        unfocusedPrefixColor = relicColor.copy(alpha = 0.7f),
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = data.targetLevel,
                    enabled = data.isEnabled,
                    onValueChange = { input ->
                        val numeric = input.filter { it.isDigit() }
                        if (numeric.isEmpty()) {
                            onDataChange(data.copy(targetLevel = ""))
                        } else {
                            var value = numeric.toIntOrNull() ?: 100
                            if (value > 100) value = 100
                            if (value < 1) value = 1
                            
                            var newCurrent = data.currentLevel.toIntOrNull() ?: 1
                            if (value in 2..newCurrent) newCurrent = value - 1
                            else if (value <= 1) newCurrent = 1

                            onDataChange(data.copy(
                                targetLevel = value.toString(),
                                currentLevel = newCurrent.toString()
                            ))
                        }
                    },
                    label = { Text("Target", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    prefix = { Text("Lv.", fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = relicColor,
                        unfocusedBorderColor = relicColor.copy(alpha = 0.5f),
                        focusedLabelColor = relicColor,
                        unfocusedLabelColor = relicColor.copy(alpha = 0.7f),
                        cursorColor = relicColor,
                        focusedPrefixColor = relicColor,
                        unfocusedPrefixColor = relicColor.copy(alpha = 0.7f),
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            if (data.type == RelicType.MAGIC_STORM && data.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = relicColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Note: Levels 1-41 are accurate; 42-100 are predictions.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = relicColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
