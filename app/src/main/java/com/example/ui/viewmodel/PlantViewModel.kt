package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.api.NongsaroApi
import com.example.data.AppDatabase
import com.example.data.Plant
import com.example.data.PlantRepository
import com.example.ml.PlantClassifier
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val temperature: Double,
        val description: String,
        val isRainyOrCloudy: Boolean
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface AnalysisUiState {
    object Idle : AnalysisUiState
    object Analyzing : AnalysisUiState
    data class Success(val result: PlantAnalysisResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface CompareUiState {
    object Idle : CompareUiState
    object Loading : CompareUiState
    data class Success(val referenceImageUri: Uri, val sourceUrl: String) : CompareUiState
    data class Error(val message: String) : CompareUiState
}

class PlantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository
    
    // 식물 상태 Flow
    val allPlants: StateFlow<List<Plant>>
    
    // 날씨 상태 Flow
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()
    
    // AI 식물 분석 상태 Flow
    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    // 캘린더 선택된 날짜 (timestamp, 오늘 기본 설정)
    private val _selectedCalendarDate = MutableStateFlow(getStartOfToday())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // 오늘 물주기 임시 체크한 리스트 저장 (체크 작동용)
    private val _wateredTodayIds = MutableStateFlow<Set<Int>>(emptySet())
    val wateredTodayIds: StateFlow<Set<Int>> = _wateredTodayIds.asStateFlow()

    // 식물 비교 상태
    private val _compareState = MutableStateFlow<CompareUiState>(CompareUiState.Idle)
    val compareState: StateFlow<CompareUiState> = _compareState.asStateFlow()

    // 챗봇 상태
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private var chatPlant: Plant? = null
    private val chatHistoryDto = mutableListOf<ChatMessageDto>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PlantRepository(database.plantDao())
        
        allPlants = repository.allPlants
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        fetchWeather()
        refreshOverdueWateringDates()
    }

    private fun refreshOverdueWateringDates() {
        viewModelScope.launch {
            val todayStart = getStartOfToday()
            val cycleMs = 24 * 60 * 60 * 1000L
            allPlants.first { it.isNotEmpty() || true }.forEach { plant ->
                if (plant.wateringCycleDays <= 0) return@forEach
                if (plant.nextWateringDate < todayStart) {
                    val newCycleMs = plant.wateringCycleDays * cycleMs
                    repository.updatePlant(plant.copy(
                        nextWateringDate = todayStart,
                    ))
                }
            }
        }
    }

    // 위치 기반 날씨 데이터 가져오기
    fun fetchWeather() {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val (lat, lon) = getCurrentLocation() ?: Pair(37.5665, 126.9780)
                Log.d("PlantViewModel", "Weather location: lat=$lat, lon=$lon")

                val response = RetrofitClient.weatherService.getCurrentWeather(
                    latitude = lat,
                    longitude = lon
                )
                val current = response.current_weather
                if (current != null) {
                    val code = current.weathercode
                    val desc = getWeatherDescription(code)
                    val isBad = code >= 50
                    _weatherState.value = WeatherUiState.Success(
                        temperature = current.temperature,
                        description = desc,
                        isRainyOrCloudy = isBad
                    )
                } else {
                    _weatherState.value = WeatherUiState.Success(24.0, "맑음", false)
                }
            } catch (e: Exception) {
                Log.e("PlantViewModel", "Weather fetch failed: ${e.message}", e)
                _weatherState.value = WeatherUiState.Success(22.5, "맑음 ☀️", false)
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val context = getApplication<Application>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.d("PlantViewModel", "Location permission not granted, using Seoul default")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                suspendCancellableCoroutine { cont ->
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(Pair(location.latitude, location.longitude))
                        } else {
                            cont.resume(null)
                        }
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantViewModel", "Location fetch failed", e)
                null
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "맑음 ☀️"
            1, 2, 3 -> "구름 조금 ☁️"
            45, 48 -> "안개 자욱 🌫️"
            51, 53, 55 -> "이슬비 보슬보슬 🌧️"
            61, 63, 65 -> "기분 좋은 비 ☔"
            71, 73, 75 -> "하얀 소복한 눈 ❄️"
            80, 81, 82 -> "시원한 소나기 🌦️"
            95, 96, 99 -> "번개 천둥 ⚡"
            else -> "화창한 날씨 🌿"
        }
    }

    // 날짜 선택
    fun selectCalendarDate(timestamp: Long) {
        _selectedCalendarDate.value = timestamp
    }

    // 오늘 물주기 임시 토글
    fun toggleWateredToday(plantId: Int) {
        val currentSet = _wateredTodayIds.value.toMutableSet()
        if (currentSet.contains(plantId)) {
            currentSet.remove(plantId)
        } else {
            currentSet.add(plantId)
        }
        _wateredTodayIds.value = currentSet
    }

    // 물주기 완료 (lastWateredDate만 업데이트, 주기는 변경하지 않음)
    fun waterPlant(plant: Plant) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cycleMs = plant.wateringCycleDays * 24 * 60 * 60 * 1000L
            val todayStart = getStartOfToday()
            val nextDate = todayStart + cycleMs
            val updated = plant.copy(lastWateredDate = now, nextWateringDate = nextDate)
            repository.updatePlant(updated)
        }
    }

    // 새로운 식물 추가 (종류가 변경됐으면 농사로 API로 재검색)
    fun addPlant(
        name: String,
        originalSpecies: String,
        nickname: String,
        wateringCycleDays: Int,
        sunlight: String,
        temperature: String,
        aiBriefing: String,
        imageUri: String?
    ) {
        viewModelScope.launch {
            val speciesChanged = name != originalSpecies

            var finalWateringCycle = wateringCycleDays
            var finalSunlight = sunlight
            var finalTemperature = temperature
            var finalBriefing = aiBriefing

            if (speciesChanged) {
                Log.d("PlantViewModel", "Species changed: $originalSpecies -> $name, re-searching API")
                val apiCare = withContext(Dispatchers.IO) {
                    try {
                        NongsaroApi.searchPlantCare(name)
                    } catch (e: Exception) {
                        Log.e("PlantViewModel", "NongsaroApi re-search error", e)
                        null
                    }
                }
                if (apiCare != null) {
                    finalWateringCycle = parseWateringCycleDays(apiCare.waterCycleSpring)
                    finalSunlight = parseLightDemand(apiCare.lightDemand)
                    finalTemperature = apiCare.growthTemperature.ifEmpty { temperature }
                    finalBriefing = buildBriefing(apiCare)
                    Log.d("PlantViewModel", "API re-search success for: $name")
                }
            }

            val now = System.currentTimeMillis()
            val todayMidnight = getStartOfToday()
            val nextDate = todayMidnight
            val newPlant = Plant(
                name = name,
                nickname = nickname,
                wateringCycleDays = finalWateringCycle,
                sunlightPreference = finalSunlight,
                temperaturePreference = finalTemperature,
                lastWateredDate = 0L,
                nextWateringDate = nextDate,
                aiBriefing = finalBriefing,
                imageUri = imageUri
            )
            repository.insertPlant(newPlant)
        }
    }

    fun deletePlant(plantId: Int) {
        viewModelScope.launch {
            repository.deletePlantById(plantId)
        }
    }

    // TFLite 모델을 사용한 식물 분석
    fun analyzePlantImage(context: Context, uri: Uri?, presetId: String? = null) {
        _analysisState.value = AnalysisUiState.Analyzing
        Log.d("PlantViewModel", "analyzePlantImage called: uri=$uri, presetId=$presetId")

        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    when {
                        uri != null -> loadBitmapFromUri(context, uri)
                        presetId != null -> createPresetBitmap()
                        else -> null
                    }
                }
                Log.d("PlantViewModel", "Bitmap loaded: ${bitmap != null}, size=${bitmap?.width}x${bitmap?.height}")

                if (bitmap == null) {
                    _analysisState.value = AnalysisUiState.Error("이미지를 불러올 수 없습니다.")
                    return@launch
                }

                Log.d("PlantViewModel", "Creating PlantClassifier...")
                val classifier = PlantClassifier(context)
                Log.d("PlantViewModel", "Classifier created, running classify...")
                val results = withContext(Dispatchers.Default) {
                    classifier.classify(bitmap)
                }
                classifier.close()
                Log.d("PlantViewModel", "Classification results: $results")

                val topResult = results.firstOrNull()
                val label = topResult?.label ?: "unknown"
                val confidence = topResult?.confidence ?: 0f
                val speciesKorean = labelToKorean(label)
                val fallbackCare = getCareInfo(label)

                val confidenceText = if (confidence < 0.3f) {
                    " (신뢰도 ${(confidence * 100).toInt()}% — 목록에 없는 식물일 수 있습니다)"
                } else {
                    ""
                }

                // 농사로 API에서 실제 식물 데이터 조회
                val searchName = labelToNongsaroName(label)
                val apiCare = withContext(Dispatchers.IO) {
                    try {
                        NongsaroApi.searchPlantCare(searchName)
                    } catch (e: Exception) {
                        Log.e("PlantViewModel", "NongsaroApi error", e)
                        null
                    }
                }
                Log.d("PlantViewModel", "NongsaroApi result: $apiCare")

                val result = if (apiCare != null) {
                    PlantAnalysisResult(
                        species = speciesKorean + confidenceText,
                        nickname_suggestion = fallbackCare.nickname,
                        watering_cycle_days = parseWateringCycleDays(apiCare.waterCycleSpring),
                        sunlight = parseLightDemand(apiCare.lightDemand),
                        temperature = apiCare.growthTemperature.ifEmpty { fallbackCare.temperature },
                        ai_care_briefing = buildBriefing(apiCare)
                    )
                } else {
                    PlantAnalysisResult(
                        species = speciesKorean + confidenceText,
                        nickname_suggestion = fallbackCare.nickname,
                        watering_cycle_days = fallbackCare.wateringCycleDays,
                        sunlight = fallbackCare.sunlight,
                        temperature = fallbackCare.temperature,
                        ai_care_briefing = fallbackCare.briefing
                    )
                }
                _analysisState.value = AnalysisUiState.Success(result)

            } catch (e: Exception) {
                Log.e("PlantViewModel", "TFLite classification error", e)
                _analysisState.value = AnalysisUiState.Error("식물 분석에 실패했습니다: ${e.message}")
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            resizeBitmap(original, 224)
        } catch (e: Exception) {
            Log.e("PlantViewModel", "Image load failed: ${e.message}", e)
            null
        }
    }

    private fun createPresetBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        return bitmap
    }

    private fun parseWateringCycleDays(waterCycleText: String): Int {
        return when {
            waterCycleText.contains("항상") || waterCycleText.contains("촉촉") -> 3
            waterCycleText.contains("표면이 말랐을때") -> 7
            waterCycleText.contains("화분 흙 대부분 말랐을때") -> 14
            else -> 7
        }
    }

    private fun parseLightDemand(lightText: String): String {
        return when {
            lightText.contains("높은 광도") && lightText.contains("중간 광도") -> "반양지 (밝은 간접광)"
            lightText.contains("높은 광도") -> "양지 (직사광선)"
            lightText.contains("중간 광도") -> "반양지 (간접광)"
            lightText.contains("낮은 광도") -> "반음지"
            else -> lightText.ifEmpty { "반양지" }
        }
    }

    private fun buildBriefing(care: PlantCareData): String {
        val lines = mutableListOf<String>()

        val springWater = care.waterCycleSpring
        val winterWater = care.waterCycleWinter
        if (springWater.isNotEmpty()) {
            lines.add("1. 봄~가을: $springWater" +
                if (winterWater.isNotEmpty() && winterWater != springWater) ", 겨울: $winterWater" else "")
        }

        if (care.lightDemand.isNotEmpty()) {
            lines.add("2. 광도: ${care.lightDemand}")
        }

        val tempInfo = buildString {
            if (care.growthTemperature.isNotEmpty()) append("생육 적온 ${care.growthTemperature}")
            if (care.winterMinTemperature.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append("겨울 최저 ${care.winterMinTemperature}")
            }
        }
        if (tempInfo.isNotEmpty()) {
            lines.add("3. $tempInfo")
        }

        if (care.humidity.isNotEmpty()) {
            lines.add("${lines.size + 1}. 습도: ${care.humidity}")
        }

        return lines.joinToString("\n").ifEmpty { "관리 정보를 불러오지 못했습니다." }
    }

    private fun labelToKorean(label: String): String {
        return when (label.lowercase()) {
            "monstera" -> "몬스테라"
            "snake_plant" -> "산세베리아"
            "pothos" -> "스킨답서스"
            "zz_plant" -> "금전수"
            "rubber_plant" -> "인도고무나무"
            else -> label
        }
    }

    private fun labelToNongsaroName(label: String): String {
        return when (label.lowercase()) {
            "monstera" -> "몬스테라"
            "snake_plant" -> "산세베리아"
            "pothos" -> "스킨답서스"
            "zz_plant" -> "금전수"
            "rubber_plant" -> "데코라고무나무"
            else -> labelToKorean(label)
        }
    }

    private data class CareInfo(
        val nickname: String,
        val wateringCycleDays: Int,
        val sunlight: String,
        val temperature: String,
        val briefing: String
    )

    private fun getCareInfo(label: String): CareInfo {
        return when (label.lowercase()) {
            "monstera" -> CareInfo(
                nickname = "몬이",
                wateringCycleDays = 7,
                sunlight = "반양지 (간접광)",
                temperature = "18~25도",
                briefing = "1. 흙 표면이 2~3cm 마를 때 물을 충분히 주세요. 과습에 주의하세요.\n2. 직사광선을 피하고 밝은 간접광이 드는 곳에 두세요.\n3. 넓은 잎에 먼지가 쌓이지 않도록 젖은 천으로 닦아주세요."
            )
            "snake_plant" -> CareInfo(
                nickname = "산이",
                wateringCycleDays = 14,
                sunlight = "반양지~반음지",
                temperature = "15~25도",
                briefing = "1. 건조에 강하므로 흙이 완전히 마른 후 2주 간격으로 물을 주세요.\n2. 밝은 간접광부터 반음지까지 잘 적응하지만, 너무 어두운 곳은 피하세요.\n3. 겨울철에는 물주기를 더 줄이고 통풍이 잘 되는 곳에 두세요."
            )
            "pothos" -> CareInfo(
                nickname = "답이",
                wateringCycleDays = 7,
                sunlight = "반음지 (간접광)",
                temperature = "18~24도",
                briefing = "1. 겉흙이 마르면 물을 주되, 화분 밑으로 물이 빠지도록 충분히 주세요.\n2. 직사광선을 피한 밝은 간접광이 좋으며, 형광등 아래에서도 잘 자랍니다.\n3. 덩굴이 길어지면 적당히 가지치기하면 더욱 풍성하게 자랍니다."
            )
            "zz_plant" -> CareInfo(
                nickname = "금이",
                wateringCycleDays = 14,
                sunlight = "반음지",
                temperature = "16~24도",
                briefing = "1. 뿌리에 수분을 저장하므로 2주에 한 번 정도 물을 주세요. 과습은 금물입니다.\n2. 반음지에서 잘 자라며 빛이 적은 실내에서도 잘 견딥니다.\n3. 잎이 윤기 있게 유지되도록 가끔 잎 표면을 닦아주세요."
            )
            "rubber_plant" -> CareInfo(
                nickname = "고무",
                wateringCycleDays = 10,
                sunlight = "반양지 (밝은 간접광)",
                temperature = "18~28도",
                briefing = "1. 겉흙이 마르면 물을 주고, 겨울에는 간격을 늘려주세요.\n2. 밝은 간접광을 좋아하며 직사광선에 오래 두면 잎이 탈 수 있습니다.\n3. 큰 잎에 먼지가 쌓이기 쉬우니 주기적으로 닦아 광합성을 도와주세요."
            )
            else -> CareInfo(
                nickname = "초록이",
                wateringCycleDays = 7,
                sunlight = "반양지",
                temperature = "18~25도",
                briefing = "1. 겉흙이 마르면 물을 충분히 주세요.\n2. 밝은 간접광이 드는 곳에 두세요.\n3. 통풍이 잘 되는 환경을 유지해주세요."
            )
        }
    }

    // 농사로 공식 이미지 다운로드 (DownloadManager 사용)
    fun downloadReferenceImage(plant: Plant) {
        _compareState.value = CompareUiState.Loading
        viewModelScope.launch {
            try {
                val baseName = plant.name.split("(").first().trim()
                val searchName = when (baseName) {
                    "인도고무나무" -> "데코라고무나무"
                    else -> baseName
                }
                Log.d("PlantViewModel", "Compare search name: $searchName (from ${plant.name})")
                val imageUrl = withContext(Dispatchers.IO) {
                    NongsaroApi.searchPlantImageUrl(searchName)
                }
                Log.d("PlantViewModel", "Compare image URL: $imageUrl")

                if (imageUrl.isNullOrEmpty()) {
                    _compareState.value = CompareUiState.Error("'${searchName}'에 대한 이미지를 농사로에서 찾을 수 없습니다. 농사로에 등록되지 않은 식물일 수 있습니다.")
                    return@launch
                }

                val context = getApplication<Application>()
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val fileName = "nongsaro_${plant.name}_${System.currentTimeMillis()}.jpg"

                val request = DownloadManager.Request(Uri.parse(imageUrl))
                    .setTitle("${plant.name} 참고 이미지")
                    .setDescription("농사로 공식 식물 이미지 다운로드 중")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GreenGreen/$fileName")

                val downloadId = dm.enqueue(request)

                withContext(Dispatchers.IO) {
                    var downloading = true
                    while (downloading) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = dm.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            when (cursor.getInt(statusIndex)) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    val localUri = cursor.getString(uriIndex)
                                    _compareState.value = CompareUiState.Success(
                                        referenceImageUri = Uri.parse(localUri),
                                        sourceUrl = imageUrl
                                    )
                                    downloading = false
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    _compareState.value = CompareUiState.Error("다운로드에 실패했습니다.")
                                    downloading = false
                                }
                            }
                        }
                        cursor.close()
                        if (downloading) {
                            kotlinx.coroutines.delay(500)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantViewModel", "Reference image download error", e)
                _compareState.value = CompareUiState.Error("이미지 다운로드 중 오류: ${e.message}")
            }
        }
    }

    fun resetCompareState() {
        _compareState.value = CompareUiState.Idle
    }

    private fun koreanToLabel(korean: String): String {
        return when (korean) {
            "몬스테라" -> "monstera"
            "산세베리아" -> "snake_plant"
            "스킨답서스" -> "pothos"
            "금전수" -> "zz_plant"
            "인도고무나무" -> "rubber_plant"
            else -> korean
        }
    }

    // 분석 상태 초기화
    fun resetAnalysisState() {
        _analysisState.value = AnalysisUiState.Idle
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // 챗봇 초기화
    fun initChat(plant: Plant) {
        if (chatPlant?.id == plant.id) return
        chatPlant = plant
        chatHistoryDto.clear()

        chatHistoryDto.add(ChatMessageDto(
            role = "system",
            content = """
                당신은 반려식물 전문 상담사입니다. 사용자의 식물에 대한 질문에 친절하고 구체적으로 답변하세요.
                현재 상담 중인 식물 정보:
                - 종류: ${plant.name}
                - 별명: ${plant.nickname}
                - 물주기: ${plant.wateringCycleDays}일 간격
                - 햇빛: ${plant.sunlightPreference}
                - 온도: ${plant.temperaturePreference}
                - 관리 팁: ${plant.aiBriefing}
                답변은 한국어로, 간결하면서도 실용적으로 해주세요. 3~5문장 정도가 적당합니다.
            """.trimIndent()
        ))

        _chatMessages.value = listOf(
            ChatMessage(
                text = "안녕하세요! ${plant.nickname}(${plant.name})에 대해 궁금한 점을 물어보세요.\n\n예: \"잎이 노래졌어요\", \"벌레가 생겼어요\", \"분갈이 시기가 궁금해요\"",
                isUser = false
            )
        )
    }

    fun sendChatMessage(userText: String) {
        val plant = chatPlant ?: return
        _chatMessages.value = _chatMessages.value + ChatMessage(text = userText, isUser = true)
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.OPENROUTER_API_KEY
                if (apiKey.isBlank()) {
                    throw IllegalStateException("OpenRouter API key is missing")
                }

                chatHistoryDto.add(ChatMessageDto(role = "user", content = userText))

                val request = ChatRequest(messages = chatHistoryDto.toList())
                val response = RetrofitClient.chatService.chat("Bearer $apiKey", request)

                val reply = response.choices?.firstOrNull()?.message?.content
                    ?: "죄송합니다, 답변을 생성하지 못했습니다."

                chatHistoryDto.add(ChatMessageDto(role = "assistant", content = reply))
                _chatMessages.value = _chatMessages.value + ChatMessage(text = reply, isUser = false)
            } catch (e: Exception) {
                Log.e("PlantViewModel", "Chat error", e)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "네트워크 오류가 발생했습니다. 다시 시도해 주세요.",
                    isUser = false
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // 오늘 시간 구하기 용 헬퍼
    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
