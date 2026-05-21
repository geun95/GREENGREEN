package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Plant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.WeatherUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    onNavigateToMyPlants: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val wateredTodayIds by viewModel.wateredTodayIds.collectAsStateWithLifecycle()

    // 오늘 물줄 식물 산출 (nextWateringDate가 오늘 자정 이전이거나 오늘 이전인 플랜트 중 아직 최종 완료안한 식물)
    val todayStart = remember { getStartOfToday() }
    val todayEnd = remember { todayStart + (24 * 60 * 60 * 1000L) }
    
    val todayWateringPlants = remember(plants) {
        plants.filter { it.nextWateringDate <= todayEnd }
    }

    // 완전히 물주기가 완료되지 않은 남은 식물의 갯수 계산
    val remainingToWaterCount = remember(todayWateringPlants, wateredTodayIds) {
        todayWateringPlants.count { !wateredTodayIds.contains(it.id) }
    }

    // 헤더에서 보여줄 인터랙티브 멘트 결정
    val headerMessage = when {
        todayWateringPlants.isEmpty() -> {
            "모두가 행복한 아침이에요.\n식물들에게 따뜻한 안사를 건네보세요."
        }
        remainingToWaterCount == 0 -> {
            "오늘의 미션을 모두 달성했어요!\n싱그러운 식물 집사님, 완벽해요 🌱"
        }
        else -> {
            "오늘 물을 줘야 할 식물이\n${remainingToWaterCount}개 남아있어요. 촉촉하게 가꿔봐요!"
        }
    }

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
                            contentDescription = "App Logo",
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "그린그린",
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            fontSize = 20.sp
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
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            
            // 1. 날씨 메세지 탑 카드 헤더 (가연그린 포인트 1)
            item {
                HeaderWeatherBox(
                    message = headerMessage,
                    weatherState = weatherState
                )
            }

            // 2. 오늘 물 줄 시간 박스 (가연그린 포인트 2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "오늘 물 줄 시간 💧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnGreenBackground
                    )
                    Text(
                        text = "전체보기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToMyPlants() }
                            .testTag("view_all_plants_button")
                    )
                }
            }

            item {
                WateringSection(
                    todayPlants = todayWateringPlants,
                    wateredTodayIds = wateredTodayIds,
                    onToggleCheck = { viewModel.toggleWateredToday(it) },
                    onCompleteWatering = { viewModel.waterPlant(it) }
                )
            }

            // 3. AI 관리 팁 리스트
            item {
                Text(
                    text = "AI 관리 팁 💡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                AiTipsSection()
            }
        }
    }
}

@Composable
fun HeaderWeatherBox(
    message: String,
    weatherState: WeatherUiState
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(GreenPrimary, GreenSecondary)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradientBrush)
            .padding(20.dp)
            .testTag("header_weather_box")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 메시지 (체크 상황에 맞춰 변경)
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "HeaderMessageAnimation"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        lineHeight = 24.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 날씨 정보 출력 박스
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Weather Icon",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "오늘의 날씨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // API 대응 날씨 출력
                when (weatherState) {
                    is WeatherUiState.Loading -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is WeatherUiState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = weatherState.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${weatherState.temperature.toInt()}°C",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is WeatherUiState.Error -> {
                        Text(
                            text = "맑음 24°C ☀️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WateringSection(
    todayPlants: List<Plant>,
    wateredTodayIds: Set<Int>,
    onToggleCheck: (Int) -> Unit,
    onCompleteWatering: (Plant) -> Unit
) {
    if (todayPlants.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("empty_watering_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = "No Plants",
                    tint = GrayTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "물을 줘야 할 식물이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        // 넘치면 박스 안에서 스크롤 (전체 보기는 ㄴㄴ 그냥 높이 규격 설정하고 스크롤 지원)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(todayPlants, key = { it.id }) { plant ->
                    val isChecked = wateredTodayIds.contains(plant.id)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                            )
                            .padding(12.dp)
                            .testTag("watering_item_${plant.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // 커스텀 체크 아이콘 버튼 (가연그린 체크기능 포인트 a)
                            IconButton(
                                onClick = { onToggleCheck(plant.id) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("checkbox_${plant.id}")
                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Check Water",
                                    tint = if (isChecked) GreenPrimary else GrayTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = plant.nickname,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) GrayTextSecondary else OnGreenBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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

                        // 물주기 완료 클릭 버튼
                        Button(
                            onClick = { onCompleteWatering(plant) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChecked) GreenSecondary else GreenPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("water_btn_${plant.id}")
                        ) {
                            Text(
                                text = if (isChecked) "수분공급완료" else "물주기",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiTipsSection() {
    val tips = remember {
        listOf(
            "실내가 지나치게 건조할 수 있어요. 몬스테라 같은 잎이 넓은 식물 주변에 가볍게 수분 분무를 촉촉하게 선사해 주세요. ✨",
            "물이 화분 받침대에 가득 고여 있으면 뿌리가 숨쉴 수 없어 썩을 수 있으니 물을 준 후 꼭 비워내시는 현명함을 강추합니다. ☝️",
            "가을 및 초겨울의 차가운 발코니 밤바람은 냉해의 원인! 밤에는 실내 거실이나 가습 존으로 모셔오는 것이 완벽해요. 🏡",
            "햇빛이 부족할 땐 인공 식물 전용 LED 조명을 활용하는 것도 식물들의 면역력과 생명력을 대폭 충전하는 우수한 팁입니다! 💡"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_tips_container"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Tip Icon",
                        tint = GoldAccent,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = OnGreenBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 오늘 자정 구하는 공용 헬퍼 함수
fun getStartOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
