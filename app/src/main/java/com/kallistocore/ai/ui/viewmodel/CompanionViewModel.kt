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
import com.kallistocore.ai.data.models.ModelCategory
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

    private val db = KallistoDatabase.getInstance(application)
    private val memoryDao = db.memoryBankDao()

    val modelManager = ModelManager(application)
    val ttsEngine = KokoroTtsEngine(application)
    val imageStudio = ImageStudioEngine(application)
    val searchController = DeviceSearchController(application)
    val llmEngine = LlmInferenceEngine(application)

    // UI Navigation & Theme State
    private val _currentTab = MutableStateFlow(MainTab.CHAT)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppThemeSetting.MIDNIGHT_DARK)
    val currentTheme: StateFlow<AppThemeSetting> = _currentTheme.asStateFlow()

    // Active Session & Chat Stream
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

    // Voice & Audio Settings
    val ttsPlaybackState: StateFlow<TtsPlaybackState> = ttsEngine.playbackState
    var selectedVoiceProfile = MutableStateFlow("af_heart (Warm American)")
    var speechSpeed = MutableStateFlow(1.0f)
    var speechPitch = MutableStateFlow(1.0f)
    var isVoiceAutoSpeak = MutableStateFlow(true)

    // Image Studio / Img2Img State
    val imageProgressState: StateFlow<ImageGenProgress> = imageStudio.progressState
    var selectedSourceImage = MutableStateFlow<Bitmap?>(null)
    var img2imgStrength = MutableStateFlow(0.75f)

    // Memory & Hardware Settings
    var allocatedMemoryBankMB = MutableStateFlow(1024) // 1 GB Default expandable
    var contextWindowSize = MutableStateFlow(4096)
    var cpuThreads = MutableStateFlow(6)
    var systemPrompt = MutableStateFlow("You are Kallisto, a sovereign, local, and helpful AI companion running offline.")

    init {
        // Initialize Default Session
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
    }

    fun setTheme(theme: AppThemeSetting) {
        _currentTheme.value = theme
    }

    /**
     * Sends a message, retrieves memory & search tools, streams tokens, and synthesizes Kokoro audio.
     */
    fun sendMessage(userText: String, sourceImage: Bitmap? = null) {
        if (userText.isBlank() && sourceImage == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = _currentSessionId.value
            val userMsgId = UUID.randomUUID().toString()

            var userImagePath: String? = null
            if (sourceImage != null) {
                val savedInput = imageStudio.saveBitmapToStorage(sourceImage, "input")
                userImagePath = savedInput.absolutePath
            }

            // Save user message to database
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

            // If user attached an image and asked to edit/generate
            if (sourceImage != null || userText.startsWith("draw ") || userText.startsWith("generate image")) {
                val generatedArt = imageStudio.generateOrEditImage(
                    prompt = userText,
                    inputImage = sourceImage,
                    strength = img2imgStrength.value
                )

                if (generatedArt != null) {
                    val aiMsgId = UUID.randomUUID().toString()
                    memoryDao.insertMessage(
                        MessageEntity(
                            id = aiMsgId,
                            sessionId = sessionId,
                            role = "assistant",
                            content = "I created/edited this image on-device for you.",
                            imageFilePath = generatedArt.absolutePath
                        )
                    )
                    return@launch
                }
            }

            // Stream LLM Response with Autonomous Memory & Search
            val aiMsgId = UUID.randomUUID().toString()
            var accumulatedText = ""

            llmEngine.streamResponse(
                userPrompt = userText,
                systemPrompt = systemPrompt.value,
                sessionId = sessionId,
                memoryDao = memoryDao,
                searchController = searchController,
                onActionDetected = { action ->
                    if (action is LlmActionRequest.LaunchApp) {
                        searchController.launchAppByName(action.appName)
                    }
                }
            ).collect { token ->
                accumulatedText += token
                // Update active message in Room
                memoryDao.insertMessage(
                    MessageEntity(
                        id = aiMsgId,
                        sessionId = sessionId,
                        role = "assistant",
                        content = accumulatedText
                    )
                )
            }

            // Synthesize voice via Kokoro TTS if enabled
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

    fun testKokoroVoice(sampleText: String = "Hello! This is a test of the Kokoro-82M neural speech engine.") {
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
        viewModelScope.launch(Dispatchers.IO) {
            modelManager.downloadModel(model)
        }
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

    override fun onCleared() {
        super.onCleared()
        ttsEngine.release()
        imageStudio.release()
    }
}
