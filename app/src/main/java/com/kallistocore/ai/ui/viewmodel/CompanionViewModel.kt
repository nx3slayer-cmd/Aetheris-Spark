package com.kallistocore.ai.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kallistocore.ai.data.db.ConversationSessionEntity
import com.kallistocore.ai.data.db.KallistoDatabase
import com.kallistocore.ai.data.db.MessageEntity
import com.kallistocore.ai.data.manager.ModelManager
import com.kallistocore.ai.data.models.AIModelInfo
import com.kallistocore.ai.data.repository.SettingsRepository
import com.kallistocore.ai.domain.image.AspectRatioOption
import com.kallistocore.ai.domain.image.ImageGenProgress
import com.kallistocore.ai.domain.image.ImageStudioEngine
import com.kallistocore.ai.domain.llm.LlmActionRequest
import com.kallistocore.ai.domain.llm.LlmInferenceEngine
import com.kallistocore.ai.domain.search.DeviceSearchController
import com.kallistocore.ai.domain.tts.KokoroTtsEngine
import com.kallistocore.ai.domain.tts.TtsPlaybackState
import com.kallistocore.ai.ui.theme.AppThemeSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UiChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String,
    val audioPath: String? = null,
    val imagePath: String? = null,
    val isGenerating: Boolean = false
)

enum class MainTab {
    CHAT,
    VOICE,
    IMAGE_STUDIO,
    SETTINGS
}

class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)
    private val db = KallistoDatabase.getInstance(application)
    private val memoryDao = db.memoryBankDao()

    val modelManager = ModelManager(application)
    val ttsEngine = KokoroTtsEngine(application)
    val imageStudio = ImageStudioEngine(application)
    val searchController = DeviceSearchController(application)
    val llmEngine = LlmInferenceEngine(application)

    private val _currentTab = MutableStateFlow(MainTab.CHAT)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentTheme = MutableStateFlow(settingsRepo.theme)
    val currentTheme: StateFlow<AppThemeSetting> = _currentTheme.asStateFlow()

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val chatMessages: StateFlow<List<UiChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            memoryDao.getMessagesForSession(sessionId).map { list ->
                list.map { entity ->
                    UiChatMessage(
                        id = entity.id,
                        text = entity.content,
                        isUser = entity.role == "user",
                        timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entity.timestamp)),
                        audioPath = entity.audioFilePath,
                        imagePath = entity.imageFilePath
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ttsPlaybackState: StateFlow<TtsPlaybackState> = ttsEngine.playbackState
    var selectedVoiceProfile = MutableStateFlow(settingsRepo.voiceProfile)
    var speechSpeed = MutableStateFlow(settingsRepo.voiceSpeed)
    var speechPitch = MutableStateFlow(settingsRepo.voicePitch)
    var isVoiceAutoSpeak = MutableStateFlow(settingsRepo.isVoiceAutoSpeak)

    val imageProgressState: StateFlow<ImageGenProgress> = imageStudio.progressState
    var selectedSourceImage = MutableStateFlow<Bitmap?>(null)
    var selectedAspectRatio = MutableStateFlow(AspectRatioOption.SQUARE_1_1)
    var selectedBaseResolution = MutableStateFlow(512)
    var img2imgUpscaleMultiplier = MutableStateFlow(1.0f)
    var forceSquareCrop = MutableStateFlow(false)
    var img2imgStrength = MutableStateFlow(0.75f)

    var systemPrompt = MutableStateFlow(settingsRepo.systemPrompt)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertSession(
                ConversationSessionEntity(
                    sessionId = _currentSessionId.value,
                    title = "New Conversation"
                )
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        viewModelScope.launch(Dispatchers.IO) {
            when (tab) {
                MainTab.CHAT -> {
                    imageStudio.clearMemoryBuffers()
                    System.gc()
                }
                MainTab.IMAGE_STUDIO -> {
                    ttsEngine.stopAudio()
                    System.gc()
                }
                else -> {}
            }
        }
    }

    fun setTheme(theme: AppThemeSetting) {
        _currentTheme.value = theme
        settingsRepo.theme = theme
    }

    fun sendMessage(userText: String, sourceImage: Bitmap? = null) {
        if (userText.isBlank() && sourceImage == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = _currentSessionId.value
            val userMsgId = UUID.randomUUID().toString()

            var userImagePath: String? = null
            if (sourceImage != null) {
                val savedInput = imageStudio.saveBitmapToStorage(sourceImage, "input")
                userImagePath = savedInput?.absolutePath
            }

            memoryDao.insertMessage(
                MessageEntity(
                    id = userMsgId,
                    sessionId = sessionId,
                    role = "user",
                    content = userText,
                    imageFilePath = userImagePath
                )
            )
            memoryDao.updateSessionTimestamp(sessionId)

            // Direct Image Generation Trigger
            if (sourceImage != null || userText.startsWith("draw ") || userText.startsWith("generate image")) {
                val prompt = userText.replace(Regex("^(draw|generate image of|generate image)\\s+", RegexOption.IGNORE_CASE), "").trim()
                val generatedArt = imageStudio.generateOrEditImage(
                    prompt = prompt,
                    inputImage = sourceImage,
                    aspectRatio = selectedAspectRatio.value,
                    baseResolution = selectedBaseResolution.value,
                    upscaleMultiplier = img2imgUpscaleMultiplier.value,
                    forceSquareCrop = forceSquareCrop.value,
                    strength = img2imgStrength.value
                )

                if (generatedArt != null) {
                    val aiMsgId = UUID.randomUUID().toString()
                    memoryDao.insertMessage(
                        MessageEntity(
                            id = aiMsgId,
                            sessionId = sessionId,
                            role = "assistant",
                            content = "I processed your image request (${imageStudio.progressState.value.outputDimensions}) on-device and saved it to DCIM/KallistoAI.",
                            imageFilePath = generatedArt.absolutePath
                        )
                    )
                    return@launch
                }
            }

            // Stream Conversational Response
            val aiMsgId = UUID.randomUUID().toString()
            var accumulatedText = ""

            llmEngine.streamResponse(
                userPrompt = userText,
                systemPrompt = systemPrompt.value,
                sessionId = sessionId,
                memoryDao = memoryDao,
                searchController = searchController
            ).collect { token ->
                accumulatedText += token
                memoryDao.insertMessage(
                    MessageEntity(
                        id = aiMsgId,
                        sessionId = sessionId,
                        role = "assistant",
                        content = accumulatedText
                    )
                )
            }

            // Autonomous Kokoro Speech Output
            if (isVoiceAutoSpeak.value && accumulatedText.isNotBlank()) {
                val audioFile = ttsEngine.synthesizeAndPlay(
                    text = accumulatedText,
                    voiceProfile = selectedVoiceProfile.value.substringBefore(" "),
                    speed = speechSpeed.value,
                    pitch = speechPitch.value
                )

                if (audioFile != null) {
                    memoryDao.insertMessage(
                        MessageEntity(
                            id = aiMsgId,
                            sessionId = sessionId,
                            role = "assistant",
                            content = accumulatedText,
                            audioFilePath = audioFile.absolutePath
                        )
                    )
                }
            }
        }
    }

    fun testKokoroVoice(sampleText: String = "Hello! Kokoro voice synthesis is active.") {
        viewModelScope.launch(Dispatchers.IO) {
            ttsEngine.synthesizeAndPlay(
                text = sampleText,
                voiceProfile = selectedVoiceProfile.value.substringBefore(" "),
                speed = speechSpeed.value,
                pitch = speechPitch.value
            )
        }
    }

    fun downloadModel(model: AIModelInfo) {
        modelManager.downloadModel(model)
    }

    fun deleteModel(model: AIModelInfo) {
        modelManager.deleteModel(model)
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMessagesBySession(_currentSessionId.value)
        }
    }

    fun pruneMemoryBank() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.pruneOldestLowPriorityMemories(100)
        }
    }
}
