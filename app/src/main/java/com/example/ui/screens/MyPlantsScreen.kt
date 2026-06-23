package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Plant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlantsScreen(
    viewModel: PlantViewModel,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Eco,
                            contentDescription = "내 반려식물",
                            tint = GreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "내 반려식물",
                            fontWeight = FontWeight.Bold,
                            color = OnGreenBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        
        if (plants.isEmpty()) {
            // 아직 등록된 식물이 없을 때 빈 화면 가이드
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GreenSurfaceVariant)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = "No Plants",
                        tint = GreenPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "등록된 초록 식물이 없습니다",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI 카메라로 사진을 분석하고\n첫 반려식물을 손쉽게 입양해 보세요!",
                    fontSize = 14.sp,
                    color = GrayTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToAddPlant,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_add_plant_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("첫 식물 등록하기", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 식물들 카드 순차 출력
                items(plants, key = { it.id }) { plant ->
                    PlantCard(
                        plant = plant,
                        onClick = { onNavigateToDetail(plant.id) }
                    )
                }

                // 등록 추가 카드 가이드라인 (Mockup 대칭성 매칭)
                item {
                    AddNewPlantCard(
                        onClick = onNavigateToAddPlant
                    )
                }
            }
        }
    }
}

@Composable
fun PlantCard(
    plant: Plant,
    onClick: () -> Unit
) {
    // 다음 물 줄 때까지 남은 D-Day 계산 (고정 주기 기반)
    val dDayText = remember(plant.nextWateringDate, plant.wateringCycleDays) {
        val todayStart = getStartOfToday()
        val baseDate = plant.nextWateringDate
        val cycleMs = plant.wateringCycleDays * 24 * 60 * 60 * 1000L

        if (cycleMs <= 0) return@remember "D-Day"

        // baseDate부터 주기를 반복해서 오늘 이후 가장 가까운 날짜 계산
        var next = baseDate
        while (next < todayStart) {
            next += cycleMs
        }
        val diffDays = ((next - todayStart) / (24 * 60 * 60 * 1000L)).toInt()
        when {
            diffDays == 0 -> "D-Day"
            else -> "D-$diffDays"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("plant_card_${plant.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // 상부 디자인 카드 영역 (D-Day 배지 포함)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(getPlantBackgroundBrush(plant.name))
            ) {
                val hasPhoto = !plant.imageUri.isNullOrEmpty() && plant.imageUri != "default"
                if (hasPhoto) {
                    AsyncImage(
                        model = Uri.parse(plant.imageUri),
                        contentDescription = plant.nickname,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Eco,
                            contentDescription = "식물",
                            tint = GreenPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // D-day 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (dDayText == "D-Day" || dDayText == "Overdue") Color(0xFFE57373)
                            else GreenPrimary
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dDayText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 하부 식물 이름 텍스트 영역
            val needsWatering = remember(plant.lastWateredDate, plant.nextWateringDate, plant.wateringCycleDays) {
                isPlantNeedsWateringToday(plant)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (needsWatering) Color(0xFFE8DCC8) else Color.Transparent)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = plant.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getPlantTypeIcon(plant.name),
                        contentDescription = null,
                        tint = getPlantTypeIconColor(plant.name),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AddNewPlantCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable { onClick() }
            .testTag("add_new_plant_shortcut_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .wrapContentSize(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add New Plant",
                    tint = GreenPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ADD NEW",
                fontSize = 12.sp,
                color = GrayTextSecondary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// 식물 종류에 맞는 이쁜 수채빛 그라데이션 제공 함수 (기본 UI 고폴리시화)
fun getPlantBackgroundBrush(speciesName: String): Brush {
    return when {
        speciesName.contains("해바라기") || speciesName.contains("Sunflower") -> {
            Brush.radialGradient(
                colors = listOf(GreenSurfaceVariant, GoldAccent)
            )
        }
        speciesName.contains("몬스테라") || speciesName.contains("Monstera") -> {
            Brush.radialGradient(
                colors = listOf(GreenTertiary, GreenPrimary)
            )
        }
        speciesName.contains("아이비") || speciesName.contains("Ivy") -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFD8F3DC), GreenSecondary)
            )
        }
        else -> {
            Brush.radialGradient(
                colors = listOf(GreenSurfaceVariant, GreenTertiary)
            )
        }
    }
}

