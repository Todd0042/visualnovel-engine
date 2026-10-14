package com.voicerpg.engine.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.model.DialogueSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Text-to-Speech Engine for the Visual Novel.
 * Powers character voices and narration using Android's on-device TextToSpeech engine.
 */
class CombatNarrator(
    private val context: Context? = null
) : TextToSpeech.OnInitListener {

    companion object {
        val DEFAULT_ALLOWED_VOICE_REGIONS = listOf("US", "CA", "GB", "AU", "IE", "ZA")

        fun getVoiceRegionCode(voice: Voice?): String {
            if (voice == null) return ""
            val country = voice.locale.country.uppercase(Locale.ROOT)
            if (country.isNotBlank()) return country
            val nameLower = voice.name.lowercase(Locale.ROOT)
            return when {
                nameLower.startsWith("en-us") || nameLower.contains("-us-") -> "US"
                nameLower.startsWith("en-gb") || nameLower.contains("-gb-") -> "GB"
                nameLower.startsWith("en-au") || nameLower.contains("-au-") -> "AU"
                nameLower.startsWith("en-ca") || nameLower.contains("-ca-") -> "CA"
                nameLower.startsWith("en-za") || nameLower.contains("-za-") -> "ZA"
                nameLower.startsWith("en-ie") || nameLower.contains("-ie-") -> "IE"
                else -> ""
            }
        }

        private val SAINT_ABBREVIATION_REGEX = Regex("""\bSt\.?\s+([A-Z][a-zA-Z]*)""")
    }

    internal fun normalizeTtsText(input: String): String {
        var text = input
        GameContent.manifest.ttsReplacements.forEach { (target, replacement) ->
            text = text.replace(target, replacement, ignoreCase = true)
        }
        text = SAINT_ABBREVIATION_REGEX.replace(text) { matchResult ->
            "Saint ${matchResult.groupValues[1]}"
        }
        return text.replace(Regex("""[ \t]+"""), " ").trim()
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var defaultVoice: Voice? = null
    private val dynamicSpeakerVoices = mutableMapOf<String, Voice?>()
    private val registeredSpeakers = mutableListOf<DialogueSpeaker>()
    private var pendingSpeechOnDone: (() -> Unit)? = null
    private var activeUtteranceId: String? = null

    private val _isNarrationEnabled = MutableStateFlow(true)
    val isNarrationEnabled: StateFlow<Boolean> = _isNarrationEnabled.asStateFlow()

    private val _speechRate = MutableStateFlow(1.05f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isCharacterPitchEnabled = MutableStateFlow(true)
    val isCharacterPitchEnabled: StateFlow<Boolean> = _isCharacterPitchEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val utteranceCounter = AtomicLong(0)

    init {
        if (context != null) {
            try {
                tts = TextToSpeech(context, this)
            } catch (_: Exception) {}
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                try {
                    engine.setAudioAttributes(audioAttributes)
                } catch (_: Exception) {}

                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    defaultVoice = engine.defaultVoice
                    assignCharacterVoices(engine)
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == activeUtteranceId) {
                            _isSpeaking.value = false
                            activeUtteranceId = null
                            val cb = pendingSpeechOnDone
                            pendingSpeechOnDone = null
                            cb?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == activeUtteranceId) {
                            _isSpeaking.value = false
                            activeUtteranceId = null
                            val cb = pendingSpeechOnDone
                            pendingSpeechOnDone = null
                            cb?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onError(utteranceId)
                    }
                })
            }
        }
    }

    fun registerSpeakers(speakers: List<DialogueSpeaker>) {
        registeredSpeakers.clear()
        registeredSpeakers.addAll(speakers)
        tts?.let { assignCharacterVoices(it) }
    }

    private fun assignCharacterVoices(engine: TextToSpeech) {
        try {
            val allVoices = engine.voices ?: emptySet()
            val physicalVoices = allVoices.filter { !it.isNetworkConnectionRequired }.ifEmpty { allVoices }
            val englishVoices = physicalVoices.filter { it.locale.language.equals("en", ignoreCase = true) }.ifEmpty { physicalVoices }

            val femaleTokens = listOf("female", "#f", "-f-", "_f_", "_female", "-f0", "smtf", "sfg", "iom", "tpd", "tpf", "gba", "gbb", "gbg", "aua", "auc", "inc")
            val maleTokens = listOf("male", "#m", "-m-", "_m_", "_male", "-m0", "smtm", "iob", "iog", "tpc", "iol", "rjs", "gbc", "gbd", "aub", "aud", "ina")

            val femaleVoices = englishVoices.filter { v -> femaleTokens.any { v.name.lowercase(Locale.ROOT).contains(it) } }
            val maleVoices = englishVoices.filter { v -> maleTokens.any { v.name.lowercase(Locale.ROOT).contains(it) } }

            val englishVoicesList = englishVoices.toList()
            val femalePool = femaleVoices.ifEmpty { englishVoices }.toList()
            val malePool = maleVoices.ifEmpty { englishVoices }.toList()

            var femaleIdx = 0
            var maleIdx = 0
            var neutralIdx = 0

            val allSpeakers = registeredSpeakers.ifEmpty { listOf(GameContent.narrator) }

            allSpeakers.forEach { speaker ->
                val char = GameContent.characterById(speaker.id)
                val isFemale = speaker.gender?.equals("female", ignoreCase = true) == true || char?.gender?.equals("female", ignoreCase = true) == true
                val isMale = speaker.gender?.equals("male", ignoreCase = true) == true || char?.gender?.equals("male", ignoreCase = true) == true

                val candidate = when {
                    speaker.isNarrator || speaker.id == "narrator" -> defaultVoice ?: englishVoicesList.firstOrNull()
                    isFemale -> femalePool[femaleIdx++ % femalePool.size]
                    isMale -> malePool[maleIdx++ % malePool.size]
                    else -> englishVoicesList[neutralIdx++ % englishVoicesList.size]
                }
                dynamicSpeakerVoices[speaker.id] = candidate
            }
        } catch (_: Exception) {}
    }

    fun getVoiceForSpeaker(speaker: DialogueSpeaker): Voice? {
        return dynamicSpeakerVoices[speaker.id] ?: defaultVoice
    }

    fun setNarrationEnabled(enabled: Boolean) {
        _isNarrationEnabled.value = enabled
        if (!enabled) stop()
    }

    fun toggleNarration(): Boolean {
        _isNarrationEnabled.value = !_isNarrationEnabled.value
        if (!_isNarrationEnabled.value) stop()
        return _isNarrationEnabled.value
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun setCharacterPitchEnabled(enabled: Boolean) {
        _isCharacterPitchEnabled.value = enabled
    }

    /**
     * Narrates a single line of visual novel dialogue aloud with character vocal pitch & rate.
     */
    fun narrateDialogue(
        speaker: DialogueSpeaker,
        text: String,
        onDone: () -> Unit = {}
    ) {
        if (!_isNarrationEnabled.value || tts == null || !isTtsInitialized) {
            onDone()
            return
        }

        val speakerVoice = getVoiceForSpeaker(speaker)
        if (speakerVoice != null) {
            try {
                tts?.voice = speakerVoice
            } catch (_: Exception) {
                try { defaultVoice?.let { tts?.voice = it } } catch (_: Exception) {}
            }
        }

        if (_isCharacterPitchEnabled.value) {
            tts?.setPitch(speaker.ttsPitch)
            tts?.setSpeechRate(_speechRate.value)
        } else {
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(_speechRate.value)
        }

        val cleanedText = text.replace("...", ". ").trim()
        val speechText = normalizeTtsText(cleanedText)

        val utteranceId = "vn_tts_${utteranceCounter.incrementAndGet()}"
        activeUtteranceId = utteranceId
        pendingSpeechOnDone = onDone
        _isSpeaking.value = true

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
            activeUtteranceId = null
            pendingSpeechOnDone = null
            onDone()
        }
    }

    fun speak(
        text: String,
        onDone: (() -> Unit)? = null
    ) {
        if (!_isNarrationEnabled.value || tts == null || !isTtsInitialized) {
            onDone?.invoke()
            return
        }

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(_speechRate.value)
        defaultVoice?.let {
            try { tts?.voice = it } catch (_: Exception) {}
        }

        val speechText = normalizeTtsText(text)
        val utteranceId = "vn_tts_${utteranceCounter.incrementAndGet()}"
        activeUtteranceId = utteranceId
        pendingSpeechOnDone = onDone
        _isSpeaking.value = true

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
            activeUtteranceId = null
            pendingSpeechOnDone = null
            onDone?.invoke()
        }
    }

    fun stop() {
        pendingSpeechOnDone = null
        activeUtteranceId = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun openVoiceSettings(activity: Activity) {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun destroy() {
        pendingSpeechOnDone = null
        activeUtteranceId = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        tts = null
    }
}
