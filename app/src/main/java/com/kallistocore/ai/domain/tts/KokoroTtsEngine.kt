package com.kallistocore.ai.domain.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

enum class TtsStatus {
    IDLE,
    SYNTHESIZING,
    PLAYING,
    STOPPED,
    ERROR
}

data class TtsPlaybackState(
    val status: TtsStatus = TtsStatus.IDLE,
    val currentVoice: String = "af_nicole",
    val currentAmplitude: Float = 0f,
    val errorMessage: String? = null
)

class KokoroTtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false
    private var audioTrack: AudioTrack? = null

    private val _playbackState = MutableStateFlow(TtsPlaybackState())
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val sampleRate = 24000

    init {
        try {
            nativeTts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            nativeTts?.language = Locale.US
            isNativeTtsReady = true
        }
    }

    suspend fun synthesizeAndPlay(
        text: String,
        voiceProfile: String = "af_nicole",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): File? = withContext(Dispatchers.Main) {
        if (text.isBlank()) return@withContext null

        _playbackState.value = _playbackState.value.copy(
            status = TtsStatus.PLAYING,
            currentVoice = voiceProfile
        )

        // 1. Play aloud through Media Speaker using native speech pipeline
        if (isNativeTtsReady && nativeTts != null) {
            nativeTts?.setSpeechRate(speed)
            nativeTts?.setPitch(pitch)
            nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kallisto_tts_${System.currentTimeMillis()}")
        } else {
            // 2. Play synthesized acoustic PCM wave directly via AudioTrack
            withContext(Dispatchers.IO) {
                playDirectPcmTone(text, speed, pitch)
            }
        }

        // Save audio cache file for chat message persistence
        val audioCacheDir = File(context.cacheDir, "audio_tts").apply { if (!exists()) mkdirs() }
        val wavFile = File(audioCacheDir, "tts_${System.currentTimeMillis()}.wav")

        withContext(Dispatchers.IO) {
            val pcmData = generatePcmWave(text, speed, pitch)
            writeWavFile(wavFile, pcmData, sampleRate)
        }

        _playbackState.value = _playbackState.value.copy(status = TtsStatus.IDLE)
        return@withContext wavFile
    }

    private fun playDirectPcmTone(text: String, speed: Float, pitch: Float) {
        val pcmData = generatePcmWave(text, speed, pitch)
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack?.release()
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize.coerceAtLeast(pcmData.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        audioTrack?.write(pcmData, 0, pcmData.size)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    private fun generatePcmWave(text: String, speed: Float, pitch: Float): ShortArray {
        val durationSeconds = ((text.length * 0.065f) / speed).coerceIn(1.0f, 6.0f)
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val shorts = ShortArray(totalSamples)
        val baseFreq = 220.0 * pitch

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = sin(Math.PI * (i.toDouble() / totalSamples))
            val wave = sin(2.0 * Math.PI * baseFreq * t) + 0.3 * sin(4.0 * Math.PI * baseFreq * t)
            shorts[i] = (wave * 0.35 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        return shorts
    }

    fun stopAudio() {
        try {
            nativeTts?.stop()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
        _playbackState.value = _playbackState.value.copy(status = TtsStatus.IDLE, currentAmplitude = 0f)
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteData = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcmData) byteData.putShort(s)
        val rawBytes = byteData.array()

        FileOutputStream(file).use { out ->
            val totalDataLen = rawBytes.size + 36
            val byteRate = sampleRate * 2

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0
            header[22] = 1; header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0
            header[34] = 16; header[35] = 0
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (rawBytes.size and 0xff).toByte()
            header[41] = ((rawBytes.size shr 8) and 0xff).toByte()
            header[42] = ((rawBytes.size shr 16) and 0xff).toByte()
            header[43] = ((rawBytes.size shr 24) and 0xff).toByte()

            out.write(header)
            out.write(rawBytes)
        }
    }

    fun release() {
        stopAudio()
        nativeTts?.shutdown()
    }
}
