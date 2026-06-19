package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.api.PlantAnalysisResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.PlantViewModel

enum class AddPlantStep {
    SELECT_IMAGE,
    ANALYZING,
    EDIT_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    viewModel: PlantViewModel,
    onNavigateBack: () -> Unit,
    onPlantCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(AddPlantStep.SELECT_IMAGE) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 분석 완료 후 저장되는 데이터 상태
    var analyzedResult by remember { mutableStateOf<PlantAnalysisResult?>(null) }
    var speciesInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }
    var wateringCycleInput by remember { mutableStateOf(3) }
    var sunlightInput by remember { mutableStateOf("양지 (직사광선)") }

    // 이미지 피커 설정
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            selectedImageUri = uri
        }
    }

    // AI 분석 상태 감지기
    LaunchedEffect(analysisState) {
        when (analysisState) {
            is AnalysisUiState.Analyzing -> {
                currentStep = AddPlantStep.ANALYZING
            }
            is AnalysisUiState.Success -> {
                val res = (analysisState as AnalysisUiState.Success).result
                analyzedResult = res
                speciesInput = res.species
                nicknameInput = res.nickname_suggestion
                wateringCycleInput = res.watering_cycle_days
                sunlightInput = res.sunlight
                currentStep = AddPlantStep.EDIT_DETAILS
            }
            is AnalysisUiState.Error -> {
                // 오류시에는 이전 단계로 팝업 후 피드백 제공 가능
                currentStep = AddPlantStep.SELECT_IMAGE
            }
            is AnalysisUiState.Idle -> {
                // 대기
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "식물 등록하기 🪴", fontWeight = FontWeight.Bold, color = OnGreenBackground) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == AddPlantStep.EDIT_DETAILS) {
                            viewModel.resetAnalysisState()
                            currentStep = AddPlantStep.SELECT_IMAGE
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = OnGreenBackground)
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
        
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "StepTransition"
        ) { step ->
            when (step) {
                AddPlantStep.SELECT_IMAGE -> {
                    SelectImageStep(
                        selectedUri = selectedImageUri,
                        onPickPhoto = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onStartAnalysis = {
                            viewModel.analyzePlantImage(context, selectedImageUri)
                        }
                    )
                }
                AddPlantStep.ANALYZING -> {
                    AnalyzingStep()
                }
                AddPlantStep.EDIT_DETAILS -> {
                    EditDetailsStep(
                        analyzedResult = analyzedResult,
                        speciesName = speciesInput,
                        onSpeciesNameChange = { speciesInput = it },
                        nickname = nicknameInput,
                        onNicknameChange = { nicknameInput = it },
                        onSave = {
                            viewModel.addPlant(
                                name = speciesInput.ifBlank { "알 수 없는 식물" },
                                originalSpecies = analyzedResult?.species ?: "",
                                nickname = nicknameInput,
                                wateringCycleDays = wateringCycleInput,
                                sunlight = sunlightInput,
                                temperature = analyzedResult?.temperature ?: "18~25도",
                                aiBriefing = analyzedResult?.ai_care_briefing ?: "정기적인 물주기와 적정 습도가 필요합니다.",
                                imageUri = selectedImageUri?.toString() ?: "default"
                            )
                            viewModel.resetAnalysisState()
                            onPlantCreated()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectImageStep(
    selectedUri: Uri?,
    onPickPhoto: () -> Unit,
    onStartAnalysis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "카메라로 찍거나 갤러리에서 선택해 주시면\nAI가 자동으로 분석해 드릴게요.",
            fontSize = 15.sp,
            color = GrayTextSecondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 2.dp,
                    color = if (selectedUri != null) GreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable { onPickPhoto() }
                .testTag("image_upload_container"),
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri != null) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected Photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Upload Icon",
                        tint = GreenPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "사진 선택하기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = onPickPhoto,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSurfaceVariant),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier.testTag("pick_image_btn")
        ) {
            Text("기기 갤러리 탐색하기", color = GreenPrimary, fontWeight = FontWeight.Bold)
        }

        // 분류 가능 식물 안내
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "현재 분류 가능한 식물 (5종)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnGreenBackground
                )
            }
            Text(
                text = "몬스테라 · 산세베리아 · 스킨답서스 · 금전수 · 인도고무나무",
                fontSize = 12.sp,
                color = GrayTextSecondary,
                lineHeight = 18.sp
            )
            Text(
                text = "위 목록에 없는 식물도 촬영 가능하며, 분석 후 종류를 직접 수정할 수 있습니다.",
                fontSize = 11.sp,
                color = GrayTextSecondary,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartAnalysis,
            enabled = selectedUri != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_analysis_btn")
        ) {
            Text(
                text = "AI 초정밀 분석 시작하기 ✨",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedUri != null) Color.White else GrayTextSecondary
            )
        }
    }
}

@Composable
fun AnalyzingStep() {
    // 회전 애니메이션 처리
    val infiniteTransition = rememberInfiniteTransition(label = "Spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // AI ANALYZING 글래스 로더 (지은그린 포인트)
        Box(
            modifier = Modifier
                .size(160.dp)
                .testTag("analyzing_spinner"),
            contentAlignment = Alignment.Center
        ) {
            // 커스텀 링 드로잉
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = GreenPrimary.copy(alpha = 0.1f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 8.dp.toPx())
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(GreenSecondary, GreenPrimary, GreenSecondary)
                    ),
                    startAngle = angle,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Text(
                text = "AI\nANALYZING",
                fontSize = 14.sp,
                color = GreenPrimary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "거의 다 됐어요!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OnGreenBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "당신의 새로운 초록 친구를\n정성스레 알아내는 중입니다...",
            fontSize = 14.sp,
            color = GrayTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EditDetailsStep(
    analyzedResult: PlantAnalysisResult?,
    speciesName: String,
    onSpeciesNameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 분석 결과 타이틀 배지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GreenSurfaceVariant)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = "분석된 식물",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "AI 분석 완료!",
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = speciesName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnGreenBackground
                    )
                }
            }
        }

        // 식물 종류 수정
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "식물 종류 (SPECIES)",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GrayTextSecondary,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = speciesName,
                onValueChange = onSpeciesNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("species_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("AI 분석 결과와 다르면 수정해 주세요") }
            )
        }

        // 별명 입력
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "식물 애칭 (NICKNAME)",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GrayTextSecondary,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nickname_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("식물의 이름을 지어주세요") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 함께하기 시작 버튼 (지은그린 포인트)
        Button(
            onClick = onSave,
            enabled = nickname.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_and_start_btn")
        ) {
            Text(
                text = "함께하기 시작!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
