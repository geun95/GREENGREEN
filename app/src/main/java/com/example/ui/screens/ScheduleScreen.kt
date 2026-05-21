package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: PlantViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()

    // 오늘 포함 +7일 달력 배열 생성 (가연그린 일정 관리 포인트)
    val calendarDays = remember {
        (0..7).map { offset ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.timeInMillis
        }
    }

    // 선택된 일자의 하루 범위 계산
    val startOfDay = selectedCalendarDate
    val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)

    // 해당 범위에 물줄 날짜가 해당하는 식물 필터링
    val scheduledPlants = remember(plants, selectedCalendarDate) {
        plants.filter { it.nextWateringDate in startOfDay until endOfDay }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "달력 일정 📅", fontWeight = FontWeight.Bold, color = OnGreenBackground) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 관리 일정 상부 소개
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "관리 일정",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnGreenBackground
                )
                Text(
                    text = "우리 초록이들의 급수 식사 시간을 체크해 두세요.",
                    fontSize = 13.sp,
                    color = GrayTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // 가로 슬라이딩 날짜 7일 피커 (가연그린 일정 4-달력 구현 포인트)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* 좌 스크롤 보조 */ }) {
                        Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null, tint = GrayTextSecondary)
                    }

                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("horizontal_calendar_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(calendarDays) { dayTimestamp ->
                            val isSelected = dayTimestamp == selectedCalendarDate
                            val dayFormat = remember { SimpleDateFormat("E", Locale.KOREAN) }
                            val dateFormat = remember { SimpleDateFormat("d", Locale.KOREAN) }
                            
                            val dayOfWeek = dayFormat.format(Date(dayTimestamp))
                            val dateNumber = dateFormat.format(Date(dayTimestamp))

                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) GreenPrimary else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.selectCalendarDate(dayTimestamp) }
                                    .padding(vertical = 10.dp)
                                    .testTag("calendar_day_$dateNumber"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = dayOfWeek,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else GrayTextSecondary
                                    )
                                    Text(
                                        text = dateNumber,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else OnGreenBackground
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { /* 우 스크롤 보조 */ }) {
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = GrayTextSecondary)
                    }
                }
            }

            // 하단 오늘의 물주기 목록 (가연그린 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedDayName = remember(selectedCalendarDate) {
                    val format = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
                    format.format(Date(selectedCalendarDate))
                }
                
                Text(
                    text = "$selectedDayName 일정 💧",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground
                )
                Text(
                    text = "${scheduledPlants.size} PLANTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            // 가연그린 일정 목록 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (scheduledPlants.isEmpty()) {
                    // 등록된 일정이 없습니다 더미 카드 (가연그린 일정 빈 영역 포인트)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("empty_schedule_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
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
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Empty Calendar",
                                tint = GrayTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "등록된 일정이 없습니다",
                                fontSize = 14.sp,
                                color = GrayTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(scheduledPlants, key = { it.id }) { plant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .clickable { onNavigateToDetail(plant.id) }
                                    .padding(14.dp)
                                    .testTag("schedule_item_${plant.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(GreenSurfaceVariant)
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        Text(getPlantEmoji(plant.name), fontSize = 20.sp)
                                    }

                                    Column {
                                        Text(
                                            text = plant.nickname,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnGreenBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${plant.name} • ${plant.wateringCycleDays}일 간격 급수 필요",
                                            fontSize = 11.sp,
                                            color = GrayTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WaterDrop,
                                        contentDescription = "Water Needs",
                                        tint = BlueWater,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "물주기",
                                        fontSize = 12.sp,
                                        color = BlueWater,
                                        fontWeight = FontWeight.Bold
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
