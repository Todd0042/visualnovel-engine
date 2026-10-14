package com.voicerpg.engine

import com.voicerpg.engine.content.GameContent
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssetWiringTest {

    private val assetsDir: File = findAssetsDir()

    private fun findAssetsDir(): File {
        val candidates = listOf(File("src/main/assets"), File("app/src/main/assets"))
        for (candidate in candidates) {
            if (candidate.isDirectory) return candidate
        }
        throw IllegalStateException(
            "Could not locate assets dir from working dir ${System.getProperty("user.dir")}"
        )
    }

    @Before
    fun setUp() {
        GameContent.reset()
        GameContent.initializeFromDirectory(File(assetsDir, "game"))
    }

    @Test
    fun everySceneBackgroundAssetResolvesToARealFile() {
        val missing = mutableListOf<String>()
        for (scene in GameContent.scenes.values) {
            val file = File(assetsDir, scene.backgroundAsset)
            if (!file.isFile) missing.add("${scene.id} -> ${scene.backgroundAsset}")
        }
        assertTrue("Missing scene background assets: $missing", missing.isEmpty())
    }

    @Test
    fun everySceneMusicAssetResolvesToARealFile() {
        val missing = mutableListOf<String>()
        for (scene in GameContent.scenes.values) {
            val file = File(assetsDir, scene.musicAsset)
            if (!file.isFile) missing.add("${scene.id} -> ${scene.musicAsset}")
        }
        assertTrue("Missing scene music assets: $missing", missing.isEmpty())
    }

    @Test
    fun everyCharacterPortraitOrSpriteResolvesToARealFile() {
        val missing = mutableListOf<String>()
        for (speaker in GameContent.allSpeakers) {
            val portrait = speaker.portraitAsset
            if (portrait != null && !File(assetsDir, portrait).isFile) {
                missing.add("${speaker.id} portrait -> $portrait")
            }
            val sprite = speaker.spriteAsset
            if (sprite != null && !File(assetsDir, sprite).isFile) {
                missing.add("${speaker.id} sprite -> $sprite")
            }
        }
        assertTrue("Missing character assets: $missing", missing.isEmpty())
    }
}