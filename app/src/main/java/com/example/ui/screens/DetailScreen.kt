package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Plant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    
    val plant = remember(plants, plantId) {
        plants.find { it.id == plantId }
    }

    if (plant == null) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        return
    }

    // 1. 생후 경과 일수 계산 (registrationDate 대비)
    val daysGrown = remember(plant.registrationDate) {
        val diffMillis = System.currentTimeMillis() - plant.registrationDate
        (diffMillis / (24 * 60 * 60 * 1000L)).toInt() + 1
    }

    // 2. 수분 밸런스 수치 계산 (주기 대비 현재 지난 날짜 계산)
    val hydrationPercentage = remember(plant.lastWateredDate, plant.wateringCycleDays) {
        val now = System.currentTimeMillis()
        val timePassed = now - plant.lastWateredDate
        val totalCycleTime = plant.wateringCycleDays * 24 * 60 * 60 * 1000L
        val ratio = 1.0f - (timePassed.toFloat() / totalCycleTime.toFloat())
        (ratio.coerceIn(0.0f, 1.0f) * 100).toInt()
    }

    val lastWateredText = remember(plant.lastWateredDate) {
        val diff = System.currentTimeMillis() - plant.lastWateredDate
        val days = (diff / (24 * 60 * 60 * 1000L)).toInt()
        if (days == 0) "오늘 물 줌" else "${days}일 전에 물 줌"
    }

    val progressValue by animateFloatAsState(
        targetValue = hydrationPercentage.toFloat() / 100f,
        animationSpec = tween(1000),
        label = "HydrationProgress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "성장 아카이브 📔", fontWeight = FontWeight.Bold, color = OnGreenBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = OnGreenBackground)
                    }
                },
                actions = {
                    // 삭제 기능 추가 제공
                    IconButton(
                        onClick = {
                            viewModel.deletePlant(plant.id)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("delete_plant_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 상단 배너 카드 (일러스트, 이름, 생후 경과)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(getPlantBackgroundBrush(plant.name))
                    .padding(20.dp)
            ) {
                // 이모지 배경 일러스트
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = getPlantEmoji(plant.name), fontSize = 72.sp)
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    // 생후 며칠 째 (지은그린 포인트)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "생후 ${daysGrown}일째 🌱",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plant.nickname,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = plant.name,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 수분 밸런스 등급 바 (수분 밸런스 - 지은그린 포인트)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hydration_balance_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "수분 밸런스 (HYDRATION)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GrayTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "현재 수분량: $hydrationPercentage%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueWater
                                )
                            }

                            // OPTIMAL 등급 표시 배지
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        when {
                                            hydrationPercentage > 40 -> GreenPrimary
                                            hydrationPercentage > 15 -> GoldAccent
                                            else -> Color(0xFFE57373)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        hydrationPercentage > 40 -> "OPTIMAL"
                                        hydrationPercentage > 15 -> "WARNING"
                                        else -> "CRITICAL THIRSTY"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        // 수분 진행 바
                        LinearProgressIndicator(
                            progress = { progressValue },
                            color = BlueWater,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = "마지막 급수일: $lastWateredText",
                            fontSize = 12.sp,
                            color = GrayTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 물 줬어요! 원형 액션 마크 버튼
                Button(
                    onClick = { viewModel.waterPlant(plant) },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("water_given_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White)
                        Text(
                            text = "물 줬어요! 🚿",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 식물 기본 스펙 속성 3종 요식업
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. CYCLE
                    SpecBox(
                        title = "CYCLE",
                        value = "${plant.wateringCycleDays}일 마다",
                        icon = Icons.Filled.RotateLeft,
                        color = BlueWater,
                        modifier = Modifier.weight(1f)
                    )
                    // 2. SUN
                    SpecBox(
                        title = "SUN",
                        value = plant.sunlightPreference,
                        icon = Icons.Filled.WbSunny,
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    // 3. TEMP
                    SpecBox(
                        title = "TEMP",
                        value = plant.temperaturePreference,
                        icon = Icons.Filled.DeviceThermostat,
                        color = Color(0xFFFF7043),
                        modifier = Modifier.weight(1f)
                    )
                }

                // AI 케어 브리핑 (지은그린 포인트)
                Text(
                    text = "AI 케어 브리핑 📢",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_care_briefing_card"),
                    colors = CardDefaults.cardColors(containerColor = GreenSurfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 뉴 라인으로 나누어진 케어 항목 출력
                        val bullets = remember(plant.aiBriefing) {
                            plant.aiBriefing.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        }

                        if (bullets.isEmpty()) {
                            Text(
                                text = "식물 아카이브 정보가 분석 중입니다.",
                                fontSize = 14.sp,
                                color = OnGreenBackground
                            )
                        } else {
                            bullets.forEach { bullet ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isNumbered = bullet.firstOrNull()?.isDigit() == true && bullet.getOrNull(1) == '.'
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(GreenPrimary)
                                            .wrapContentSize(Alignment.Center),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isNumbered) bullet.first().toString() else "🌱",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    val cleanedText = if (isNumbered) {
                                        bullet.substring(2).trim()
                                    } else {
                                        bullet
                                    }

                                    Text(
                                        text = cleanedText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnGreenBackground,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GrayTextSecondary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnGreenBackground,
                maxLines = 1
            )
        }
    }
}
