package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // 식물 종류 (예: 해바라기, 몬스테라)
    val nickname: String, // 별명 (예: 바라기짱)
    val wateringCycleDays: Int, // 물주기 주기 (며칠 간격)
    val sunlightPreference: String, // 햇빛 선호도 (예: 양지, 반음지 등)
    val temperaturePreference: String, // 온도 (예: 18~25도)
    val lastWateredDate: Long, // 마지막으로 물 준 날짜 (timestamp)
    val nextWateringDate: Long, // 다음 물 줄 날짜 (timestamp, calculated as lastWateredDate + cycle in millis)
    val registrationDate: Long = System.currentTimeMillis(), // 등록일 (timestamp)
    val imageUri: String? = null, // 카메라/갤러리 사진 경로 혹은 preset 리소스 식별자
    val aiBriefing: String, // AI 관리 조언 브리핑
    val isWateredTodayPassed: Boolean = false // 오늘 임시 체크 완료 여부
)
