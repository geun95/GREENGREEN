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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Plant
import com.example.ui.theme.*
import com.example.ui.viewmodel.CompareUiState
import com.example.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val compareState by viewModel.compareState.collectAsStateWithLifecycle()

    val plant = remember(plants, plantId) {
        plants.find { it.id == plantId }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetCompareState() }
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

    val needsWatering = remember(plant.lastWateredDate, plant.nextWateringDate, plant.wateringCycleDays) {
        isPlantNeedsWateringToday(plant)
    }
    val detailBgColor = if (needsWatering) Color(0xFFE8DCC8) else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = "성장 아카이브",
                            tint = GreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "성장 아카이브",
                            fontWeight = FontWeight.Bold,
                            color = OnGreenBackground
                        )
                    }
                },
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
                    containerColor = detailBgColor
                )
            )
        },
        containerColor = detailBgColor,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 상단 배너 카드 (사진, 이름, 생후 경과)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Eco,
                            contentDescription = "식물",
                            tint = GreenPrimary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // 텍스트 가독성을 위한 하단 그라데이션
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "생후 ${daysGrown}일째",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Filled.Grass,
                                contentDescription = "새싹",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        .height(46.dp)
                        .testTag("water_given_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "물 줬어요!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(imageVector = Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White)
                    }
                }

                // AI 상담하기 + 내 식물 비교하기 (한 줄)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToChat(plant.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        border = BorderStroke(1.5.dp, GreenPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("ai_chat_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.ChatBubble, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "AI 상담", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }

                    OutlinedButton(
                        onClick = { viewModel.downloadReferenceImage(plant) },
                        enabled = compareState !is CompareUiState.Loading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        border = BorderStroke(1.5.dp, GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("compare_plant_btn")
                    ) {
                        if (compareState is CompareUiState.Loading) {
                            CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Filled.Compare, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (compareState is CompareUiState.Loading) "다운로드 중..." else "식물 비교",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent
                        )
                    }
                }

                // 비교 결과 카드
                when (val state = compareState) {
                    is CompareUiState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🔍 식물 비교",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnGreenBackground
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 내 식물 사진
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "내 식물",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val hasPhoto = !plant.imageUri.isNullOrEmpty() && plant.imageUri != "default"
                                            if (hasPhoto) {
                                                AsyncImage(
                                                    model = Uri.parse(plant.imageUri),
                                                    contentDescription = "내 식물",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Eco,
                                                    contentDescription = null,
                                                    tint = GreenPrimary,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 농사로 공식 사진
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "공식 사진",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = state.referenceImageUri,
                                                contentDescription = "농사로 공식 이미지",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "농사로(nongsaro.go.kr) 제공 이미지와 비교해 보세요.\n다운로드 폴더에도 저장되었습니다.",
                                    fontSize = 11.sp,
                                    color = GrayTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    is CompareUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                    else -> {}
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
