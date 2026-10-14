package com.voicerpg.engine

import com.voicerpg.engine.engine.SaveManager
import com.voicerpg.engine.model.DialogueLogEntry
import com.voicerpg.engine.model.GameSaveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveSystemTest {

    private lateinit var saveManager: SaveManager

    @Before
    fun setUp() {
        saveManager = SaveManager(context = null) // Headless in-memory persistence
    }

    @Test
    fun testInitialSaveCreation() {
        val save = saveManager.createInitialSave(slot = 1)
        assertTrue(saveManager.hasSave(1))
        assertNotNull(save.currentSceneId)
        assertNotNull(save.currentNodeId)
    }

    @Test
    fun testMultiSlotIndependence() {
        val save1 = GameSaveData(currentSceneId = "scene_1", currentNodeId = "node_1")
        val save2 = GameSaveData(currentSceneId = "scene_2", currentNodeId = "node_2")
        val save3 = GameSaveData(currentSceneId = "scene_3", currentNodeId = "node_3")

        saveManager.save(save1, slot = 1)
        saveManager.save(save2, slot = 2)
        saveManager.save(save3, slot = 3)

        assertTrue(saveManager.hasSave(1))
        assertTrue(saveManager.hasSave(2))
        assertTrue(saveManager.hasSave(3))

        assertEquals("scene_1", saveManager.load(1)?.currentSceneId)
        assertEquals("scene_2", saveManager.load(2)?.currentSceneId)
        assertEquals("scene_3", saveManager.load(3)?.currentSceneId)

        saveManager.deleteSave(2)
        assertFalse(saveManager.hasSave(2))
        assertNull(saveManager.load(2))

        assertTrue(saveManager.hasSave(1))
        assertTrue(saveManager.hasSave(3))
    }

    @Test
    fun testDialogueBacklogCappedAt150() {
        val oversizedLog = (1..200).map { i ->
            DialogueLogEntry(
                speakerId = "speaker_$i",
                speakerName = "Speaker $i",
                text = "Line $i"
            )
        }
        val save = GameSaveData(
            currentSceneId = "scene_1",
            currentNodeId = "node_1",
            recentDialogueLog = oversizedLog
        )
        saveManager.save(save, slot = 1)

        val loaded = saveManager.load(1)
        assertNotNull(loaded)
        assertEquals(150, loaded!!.recentDialogueLog.size)
        assertEquals("Line 51", loaded.recentDialogueLog.first().text)
        assertEquals("Line 200", loaded.recentDialogueLog.last().text)
    }
}
