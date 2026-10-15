package com.voicerpg.engine.content

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.voicerpg.engine.model.DialogueNode
import com.voicerpg.engine.model.DialogueSpeaker
import com.voicerpg.engine.model.SpeakerSide
import com.voicerpg.engine.model.StoryScene
import java.io.File

// =============================================================================
// JSON SCHEMA DTOs (assets/game/**)
// =============================================================================

data class ManifestDto(
    val gameTitle: String = "Visual Novel",
    val gameSubtitle: String = "",
    val initialSceneId: String = "",
    val initialNodeId: String = "",
    val titleBackgroundAsset: String? = null,
    val ttsReplacements: Map<String, String>? = null
)

data class CharacterDto(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val gender: String? = null,
    val portraitAsset: String? = null,
    val spriteAsset: String? = null,
    val themeColor: String = "#FFFFFF",
    val ttsPitch: Float = 1.0f,
    val samplePhrase: String? = null,
    val allowedVoiceRegions: List<String>? = null
)

data class StorySceneDto(
    val id: String = "",
    val name: String = "",
    val chapterTitle: String = "",
    val backgroundAsset: String = "",
    val initialNodeId: String = "",
    val ambientDescription: String = "",
    val musicAsset: String = "audio/music/bgm_exploration.ogg",
    val chapterIndex: Int = 0,
    val actNumber: Int = 1,
    val actTitle: String = "Act I"
)



data class DialogueNodeDto(
    val id: String = "",
    @SerializedName(value = "speakerId", alternate = ["speaker_id", "speaker"])
    val speakerId: String = "narrator",
    @SerializedName(value = "speakerSide", alternate = ["speaker_side", "side"])
    val speakerSide: String? = null,
    val text: String = "",
    @SerializedName(value = "nextNodeId", alternate = ["next_node_id"])
    val nextNodeId: String? = null,
    @SerializedName(value = "changeSceneId", alternate = ["change_scene_id"])
    val changeSceneId: String? = null,
    @SerializedName(value = "backgroundAsset", alternate = ["background_asset"])
    val backgroundAsset: String? = null,
    @SerializedName(value = "musicAsset", alternate = ["music_asset"])
    val musicAsset: String? = null,
    @SerializedName(value = "sfxAsset", alternate = ["sfx_asset"])
    val sfxAsset: String? = null
)

data class GameManifest(
    val gameTitle: String = "Visual Novel",
    val gameSubtitle: String = "",
    val initialSceneId: String = "",
    val initialNodeId: String = "",
    val titleBackgroundAsset: String? = null,
    val ttsReplacements: Map<String, String> = emptyMap()
)

data class GameContentPack(
    val manifest: GameManifest,
    val characters: Map<String, CharacterDto>,
    val scenes: Map<String, StorySceneDto>,
    val nodes: Map<String, DialogueNodeDto>
)

// =============================================================================
// CONTENT LOADER & REPOSITORY
// =============================================================================

object GameContent {

    private val gson = Gson()

    @Volatile
    private var packState: GameContentPack = emergencyFallbackPack()

    val pack: GameContentPack get() = packState
    val manifest: GameManifest get() = packState.manifest
    val characters: Map<String, CharacterDto> get() = packState.characters

    private val convertedScenes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        packState.scenes.mapValues { (_, dto) -> toStoryScene(dto) }
    }
    val scenes: Map<String, StoryScene> get() = convertedScenes

    private val convertedNodes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val speakerMap = packState.characters.mapValues { (_, dto) -> toDialogueSpeaker(dto) }
        packState.nodes.mapValues { (_, dto) ->
            toDialogueNode(dto, speakerMap)
        }
    }
    val nodes: Map<String, DialogueNode> get() = convertedNodes

    val allSpeakers: List<DialogueSpeaker>
        get() = packState.characters.values.map { toDialogueSpeaker(it) }

    val narrator: DialogueSpeaker
        get() = speakerById("narrator") ?: DialogueSpeaker.NARRATOR

    val initialScene: StoryScene
        get() = scenes[manifest.initialSceneId] ?: scenes.values.firstOrNull() ?: emergencyFallbackScene()

    val initialNode: DialogueNode
        get() = nodes[manifest.initialNodeId] ?: nodes.values.firstOrNull() ?: emergencyFallbackNode()

    fun speakerById(id: String): DialogueSpeaker? {
        val dto = packState.characters[id] ?: return null
        return toDialogueSpeaker(dto)
    }

    fun characterById(id: String): CharacterDto? = packState.characters[id]

    fun sceneById(id: String): StoryScene? = scenes[id]

    fun nodeById(id: String): DialogueNode? = nodes[id]

    fun initialize(context: Context) {
        try {
            val loaded = GameContentLoader.loadFromAssets(context.assets, "game")
            packState = loaded
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initializeFromDirectory(gameDir: File) {
        try {
            packState = GameContentLoader.loadFromDirectory(gameDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        packState = emergencyFallbackPack()
    }

    private fun toStoryScene(dto: StorySceneDto): StoryScene {
        return StoryScene(
            id = dto.id,
            name = dto.name,
            chapterTitle = dto.chapterTitle,
            backgroundAsset = dto.backgroundAsset,
            initialNodeId = dto.initialNodeId,
            ambientDescription = dto.ambientDescription,
            musicAsset = dto.musicAsset,
            chapterIndex = dto.chapterIndex,
            actNumber = dto.actNumber,
            actTitle = dto.actTitle
        )
    }

    private fun toDialogueSpeaker(dto: CharacterDto): DialogueSpeaker {
        val color = try {
            Color(android.graphics.Color.parseColor(dto.themeColor))
        } catch (_: Exception) {
            Color.White
        }
        return DialogueSpeaker(
            id = dto.id,
            name = dto.name,
            title = dto.title,
            portraitAsset = dto.portraitAsset,
            spriteAsset = dto.spriteAsset,
            themeColor = color,
            ttsPitch = dto.ttsPitch,
            samplePhrase = dto.samplePhrase,
            gender = dto.gender,
            allowedVoiceRegions = dto.allowedVoiceRegions ?: emptyList()
        )
    }

    private fun toDialogueNode(dto: DialogueNodeDto, speakerMap: Map<String, DialogueSpeaker>): DialogueNode {
        val speaker = speakerMap[dto.speakerId] ?: DialogueSpeaker.NARRATOR
        val side = when (dto.speakerSide?.uppercase()) {
            "RIGHT" -> SpeakerSide.RIGHT
            "CENTER_NARRATOR" -> SpeakerSide.CENTER_NARRATOR
            else -> SpeakerSide.LEFT
        }
        return DialogueNode(
            id = dto.id,
            speaker = speaker,
            side = side,
            text = dto.text,
            nextNodeId = dto.nextNodeId,
            changeSceneId = dto.changeSceneId,
            backgroundAsset = dto.backgroundAsset,
            musicAsset = dto.musicAsset,
            sfxAsset = dto.sfxAsset
        )
    }

    private fun emergencyFallbackPack(): GameContentPack {
        val manifest = GameManifest(
            gameTitle = "Visual Novel",
            initialSceneId = "scene_start",
            initialNodeId = "node_start"
        )
        val defaultSpeaker = CharacterDto(
            id = "narrator",
            name = "Narrator",
            title = "Chronicle"
        )
        val defaultScene = StorySceneDto(
            id = "scene_start",
            name = "Opening Scene",
            chapterTitle = "Chapter I",
            backgroundAsset = "backgrounds/plate_forest.png",
            initialNodeId = "node_start"
        )
        val defaultNode = DialogueNodeDto(
            id = "node_start",
            speakerId = "narrator",
            text = "The story begins..."
        )
        return GameContentPack(
            manifest = manifest,
            characters = mapOf("narrator" to defaultSpeaker),
            scenes = mapOf("scene_start" to defaultScene),
            nodes = mapOf("node_start" to defaultNode)
        )
    }

    private fun emergencyFallbackScene() = StoryScene(
        id = "scene_start",
        name = "Opening Scene",
        chapterTitle = "Chapter I",
        backgroundAsset = "backgrounds/plate_forest.png",
        initialNodeId = "node_start"
    )

    private fun emergencyFallbackNode() = DialogueNode(
        id = "node_start",
        speaker = DialogueSpeaker.NARRATOR,
        text = "The story begins..."
    )
}

object GameContentLoader {

    private val gson = Gson()

    fun loadFromAssets(assetManager: AssetManager, basePath: String): GameContentPack {
        val source = AssetContentSource(assetManager, basePath)
        return load(source)
    }

    fun loadFromDirectory(directory: File): GameContentPack {
        val source = DirectoryContentSource(directory)
        return load(source)
    }

    private fun load(source: ContentSource): GameContentPack {
        // 1. Manifest
        val manifestDto = source.readJson("manifest.json", ManifestDto::class.java) ?: ManifestDto()
        val manifest = GameManifest(
            gameTitle = manifestDto.gameTitle,
            gameSubtitle = manifestDto.gameSubtitle,
            initialSceneId = manifestDto.initialSceneId,
            initialNodeId = manifestDto.initialNodeId,
            titleBackgroundAsset = manifestDto.titleBackgroundAsset,
            ttsReplacements = manifestDto.ttsReplacements ?: emptyMap()
        )

        // 2. Characters
        val characters = mutableMapOf<String, CharacterDto>()
        source.listSubFiles("characters").forEach { path ->
            if (path.endsWith(".json")) {
                source.readJson(path, CharacterDto::class.java)?.let { char ->
                    if (char.id.isNotBlank()) characters[char.id] = char
                }
            }
        }

        // 3. Scenes
        val scenes = mutableMapOf<String, StorySceneDto>()
        val storyFiles = source.listSubFiles("story")
        val sceneFiles = (storyFiles.filter { it.substringAfterLast("/").startsWith("scenes") && it.endsWith(".json") } +
                source.listSubFiles("story/scenes").filter { it.endsWith(".json") }).distinct()
        val effectiveSceneFiles = sceneFiles.ifEmpty { listOf("story/scenes.json") }

        for (sceneFilePath in effectiveSceneFiles) {
            parseScenes(source, sceneFilePath).forEach { scene ->
                if (scene.id.isNotBlank()) scenes[scene.id] = scene
            }
        }

        // 4. Dialogue Nodes (Modular split loading: nodes.json, nodes_act1.json, etc.)
        val nodes = mutableMapOf<String, DialogueNodeDto>()
        val nodeFiles = (storyFiles.filter { it.substringAfterLast("/").startsWith("nodes") && it.endsWith(".json") } +
                source.listSubFiles("story/nodes").filter { it.endsWith(".json") }).distinct()
        val effectiveNodeFiles = nodeFiles.ifEmpty { listOf("story/nodes.json") }

        for (nodeFilePath in effectiveNodeFiles) {
            parseNodes(source, nodeFilePath).forEach { node ->
                if (node.id.isNotBlank()) nodes[node.id] = node
            }
        }

        return GameContentPack(
            manifest = manifest,
            characters = characters,
            scenes = scenes,
            nodes = nodes
        )
    }

    private fun parseScenes(source: ContentSource, path: String): List<StorySceneDto> {
        return try {
            val element = source.readJson(path, com.google.gson.JsonElement::class.java) ?: return emptyList()
            when {
                element.isJsonArray -> {
                    gson.fromJson(element, Array<StorySceneDto>::class.java)?.toList() ?: emptyList()
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    if (obj.has("scenes")) {
                        gson.fromJson(obj.get("scenes"), Array<StorySceneDto>::class.java)?.toList() ?: emptyList()
                    } else {
                        gson.fromJson(obj, StorySceneDto::class.java)?.let { listOf(it) } ?: emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNodes(source: ContentSource, path: String): List<DialogueNodeDto> {
        return try {
            val element = source.readJson(path, com.google.gson.JsonElement::class.java) ?: return emptyList()
            when {
                element.isJsonArray -> {
                    gson.fromJson(element, Array<DialogueNodeDto>::class.java)?.toList() ?: emptyList()
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    if (obj.has("nodes")) {
                        gson.fromJson(obj.get("nodes"), Array<DialogueNodeDto>::class.java)?.toList() ?: emptyList()
                    } else {
                        gson.fromJson(obj, DialogueNodeDto::class.java)?.let { listOf(it) } ?: emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

interface ContentSource {
    fun <T> readJson(relativePath: String, clazz: Class<T>): T?
    fun listSubFiles(relativeDir: String): List<String>
}

class AssetContentSource(
    private val assetManager: AssetManager,
    private val basePath: String
) : ContentSource {
    private val gson = Gson()

    override fun <T> readJson(relativePath: String, clazz: Class<T>): T? {
        val fullPath = if (basePath.isBlank()) relativePath else "$basePath/$relativePath"
        return try {
            assetManager.open(fullPath).bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, clazz)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun listSubFiles(relativeDir: String): List<String> {
        val fullDir = if (basePath.isBlank()) relativeDir else "$basePath/$relativeDir"
        return try {
            val list = assetManager.list(fullDir) ?: emptyArray()
            list.map { "$relativeDir/$it" }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

class DirectoryContentSource(
    private val baseDir: File
) : ContentSource {
    private val gson = Gson()

    override fun <T> readJson(relativePath: String, clazz: Class<T>): T? {
        val file = File(baseDir, relativePath)
        if (!file.exists()) return null
        return try {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, clazz)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun listSubFiles(relativeDir: String): List<String> {
        val dir = File(baseDir, relativeDir)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.map { "$relativeDir/${it.name}" } ?: emptyList()
    }
}
