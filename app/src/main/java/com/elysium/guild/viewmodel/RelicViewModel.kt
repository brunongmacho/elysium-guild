package com.elysium.guild.viewmodel

import androidx.lifecycle.ViewModel
import com.elysium.guild.ui.screens.RelicData
import com.elysium.guild.ui.screens.RelicType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RelicViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RelicUiState())
    val uiState: StateFlow<RelicUiState> = _uiState.asStateFlow()

    val standardCosts = listOf(
        15, 25, 36, 48, 62, 78, 96, 116, 138, 163, 192, 225, 263, 306, 355, 410, 471, 539, 614, 697,
        790, 890, 990, 1090, 1190, 1340, 1490, 1760, 1964, 2172, 2400, 2650, 2924, 3224, 3552, 3910,
        4300, 4724, 5184, 5682, 6220, 6800, 7424, 8094, 8812, 9580, 10400, 11274, 12204, 13192, 14240,
        15350, 16524, 17764, 19072, 20450, 21900, 23424, 25024, 26702, 28460, 30300, 32224, 34234,
        36332, 38520, 40800, 43174, 45644, 48212, 50880, 53650, 56524, 59504, 62592, 65790, 69100,
        72524, 76064, 79722, 83500, 87400, 91424, 95574, 99852, 104260, 108800, 113474, 118284, 123232,
        128320, 133550, 138924, 144444, 150112, 155930, 161900, 168024, 174304
    )

    val magicStormCosts = listOf(
        4999, 5104, 5220, 5347, 5486, 5636, 5799, 5977, 6170, 6381, 6611, 6863, 7138, 7438, 7765, 8122,
        8511, 8934, 9393, 9891, 10428, 11008, 11632, 12303, 13022, 13791, 14611, 15486, 16417, 17405,
        18453, 19562, 20734, 21972, 23278, 24654, 26102, 27626, 29228, 30912, 32681, 34540, 36492,
        38542, 40697, 42961, 45341, 47845, 50481, 53256, 56181, 59267, 62525, 65967, 69607, 73460,
        77543, 81873, 86468, 91351, 96542, 102066, 107949, 114219, 120904, 128037, 135652, 143786,
        152476, 161765, 171695, 182315, 193673, 205822, 218818, 232719, 247587, 263489, 280494, 298675,
        318108, 338876, 361062, 384757, 410054, 437052, 465855, 496569, 529310, 564194, 601346, 640896,
        682977, 727732, 775307, 825855, 879535, 936514, 996965
    )

    init {
        updateTotals()
    }

    fun updateRelic(index: Int, data: RelicData) {
        _uiState.update { currentState ->
            val newList = currentState.relics.toMutableList()
            if (index in newList.indices) {
                newList[index] = data
            }
            currentState.copy(relics = newList)
        }
        updateTotals()
    }

    fun updateOwnedPieces(owned: String) {
        _uiState.update { it.copy(ownedPieces = owned) }
        updateTotals()
    }

    private fun updateTotals() {
        _uiState.update { currentState ->
            val totalCost = currentState.relics.sumOf { data ->
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

            val owned = currentState.ownedPieces.toLongOrNull() ?: 0L
            val remaining = (totalCost - owned).coerceAtLeast(0L)
            val marketPacks = calculateOptimalPacks(remaining)

            currentState.copy(
                totalCost = totalCost,
                remainingNeeded = remaining,
                marketShoppingList = marketPacks
            )
        }
    }

    private fun calculateOptimalPacks(needed: Long): List<MarketPack> {
        var remaining = needed
        val tiers = listOf(
            MarketPackType.T5,
            MarketPackType.T4,
            MarketPackType.T3,
            MarketPackType.T2,
            MarketPackType.T1
        )

        val result = mutableListOf<MarketPack>()
        for (tier in tiers) {
            if (remaining >= tier.amount) {
                val count = (remaining / tier.amount).toInt()
                result.add(MarketPack(tier, count))
                remaining %= tier.amount
            }
        }
        // If there's still something remaining, add one smallest pack
        if (remaining > 0) {
            val t1Index = result.indexOfFirst { it.type == MarketPackType.T1 }
            if (t1Index != -1) {
                result[t1Index] = result[t1Index].copy(count = result[t1Index].count + 1)
            } else {
                result.add(MarketPack(MarketPackType.T1, 1))
            }
        }
        return result.sortedByDescending { it.type.amount }
    }
}

data class RelicUiState(
    val relics: List<RelicData> = listOf(
        RelicData(RelicType.ORIGIN_OF_DESTRUCTION, "1", "100"),
        RelicData(RelicType.BARRIER_PROTECTION, "1", "100"),
        RelicData(RelicType.CRYSTAL_OF_LIFE, "1", "100"),
        RelicData(RelicType.MAGIC_STORM, "1", "100")
    ),
    val ownedPieces: String = "0",
    val totalCost: Long = 0,
    val remainingNeeded: Long = 0,
    val marketShoppingList: List<MarketPack> = emptyList()
)

data class MarketPack(
    val type: MarketPackType,
    val count: Int
)

enum class MarketPackType(val displayName: String, val amount: Int) {
    T1("T1", 1000),
    T2("T2", 5000),
    T3("T3", 10000),
    T4("T4", 50000),
    T5("T5", 100000)
}
