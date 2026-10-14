package com.voicerpg.engine.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicerpg.engine.content.GameContent
import com.voicerpg.engine.model.GameSaveData
import com.voicerpg.engine.model.SaveSlotInfo
import com.voicerpg.engine.model.SaveSummary
import java.io.File
import java.io.FileOutputStream

/**
 * Robust Persistence Manager for Visual Novel story progress.
 * Uses atomic file operations to prevent file corruption.
 * Supports up to 3 save slots plus a dedicated auto-save slot.
 */
class SaveManager(private val context: Context? = null) {

    companion object {
        const val MAX_SLOTS = 3
        const val DEFAULT_SLOT = 1
        const val AUTO_SAVE_SLOT = 0
        fun getSlotFileName(slot: Int): String = if (slot == AUTO_SAVE_SLOT) "save_auto.json" else "save_slot_$slot.json"
    }

    fun sanitizeSlot(slot: Int): Int = if (slot == AUTO_SAVE_SLOT) AUTO_SAVE_SLOT else slot.coerceIn(1, MAX_SLOTS)

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    var currentSlot: Int = DEFAULT_SLOT
        set(value) {
            field = sanitizeSlot(value)
        }

    // In-memory slot storage for unit tests and headless JVM environments
    private val inMemorySlots = mutableMapOf<Int, String>()

    private fun getSaveFile(slot: Int): File? {
        val validSlot = sanitizeSlot(slot)
        return context?.let { File(it.filesDir, getSlotFileName(validSlot)) }
    }

    private fun writeAtomic(file: File, content: String) {
        val parentDir = file.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${file.name}.tmp")
        var stream: FileOutputStream? = null
        try {
            stream = FileOutputStream(tempFile)
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.close()
            stream = null
            if (!tempFile.renameTo(file)) {
                throw Exception("Failed to rename temp file to target")
            }
        } catch (e: Exception) {
            if (stream != null) {
                try { stream.close() } catch (_: Exception) {}
            }
            tempFile.delete()
            throw e
        }
    }

    private fun readAtomic(file: File): String? {
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasSave(slot: Int = currentSlot): Boolean {
        val validSlot = sanitizeSlot(slot)
        val file = getSaveFile(validSlot)
        return if (file != null) {
            file.exists() && file.length() > 0
        } else {
            !inMemorySlots[validSlot].isNullOrBlank()
        }
    }

    fun save(data: GameSaveData, slot: Int = currentSlot, updateTimestamp: Boolean = true): Boolean {
        val validSlot = sanitizeSlot(slot)
        return try {
            val cappedLog = if (data.recentDialogueLog.size > 150) {
                data.recentDialogueLog.takeLast(150)
            } else {
                data.recentDialogueLog
            }
            val updated = data.copy(
                saveTimestamp = if (updateTimestamp) System.currentTimeMillis() else data.saveTimestamp,
                recentDialogueLog = cappedLog
            )
            val json = gson.toJson(updated)
            val file = getSaveFile(validSlot)
            if (file != null) {
                writeAtomic(file, json)
            } else {
                inMemorySlots[validSlot] = json
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun load(slot: Int = currentSlot): GameSaveData? {
        val validSlot = sanitizeSlot(slot)
        return try {
            val file = getSaveFile(validSlot)
            val json = if (file != null) {
                readAtomic(file)
            } else {
                inMemorySlots[validSlot]
            }

            if (json.isNullOrBlank()) return null
            gson.fromJson(json, GameSaveData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteSave(slot: Int = currentSlot): Boolean {
        val validSlot = sanitizeSlot(slot)
        inMemorySlots.remove(validSlot)
        val file = getSaveFile(validSlot)
        return if (file != null) {
            file.delete()
            true
        } else {
            true
        }
    }

    fun getSlotSummary(slot: Int): SaveSummary? {
        val data = load(slot) ?: return null
        val scene = GameContent.scenes[data.currentSceneId]
            ?: GameContent.initialScene
        return SaveSummary(
            slotIndex = slot,
            chapterTitle = scene.chapterTitle,
            sceneName = scene.name,
            timestamp = data.saveTimestamp
        )
    }

    fun getAllSlotInfos(): List<SaveSlotInfo> {
        return (1..MAX_SLOTS).map { slot ->
            SaveSlotInfo(
                slotIndex = slot,
                summary = getSlotSummary(slot)
            )
        }
    }

    fun createInitialSave(slot: Int = currentSlot): GameSaveData {
        val validSlot = sanitizeSlot(slot)
        val manifest = GameContent.manifest

        val newSave = GameSaveData(
            currentSceneId = manifest.initialSceneId,
            currentNodeId = manifest.initialNodeId,
            visitedNodeIds = emptyList()
        )

        save(newSave, validSlot)
        return newSave
    }
}
