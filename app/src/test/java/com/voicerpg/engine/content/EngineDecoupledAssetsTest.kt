package com.voicerpg.engine.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EngineDecoupledAssetsTest {

    private fun loadPack(): GameContentPack {
        val assetsDir = listOf(
            File("app/src/main/assets/game"),
            File("src/main/assets/game"),
            File("assets/game")
        ).firstOrNull { it.isDirectory }
        assertNotNull("Assets directory should be found", assetsDir)
        GameContent.reset()
        GameContent.initializeFromDirectory(assetsDir!!)
        return GameContent.pack
    }

    @Test
    fun testManifestFields() {
        val pack = loadPack()
        val manifest = pack.manifest

        assertTrue("gameTitle must not be blank", manifest.gameTitle.isNotBlank())
        assertTrue("initialSceneId must not be blank", manifest.initialSceneId.isNotBlank())
        assertTrue("initialNodeId must not be blank", manifest.initialNodeId.isNotBlank())
    }

    @Test
    fun testScenesLoadedAndAssetsExist() {
        val pack = loadPack()
        assertTrue("Scenes must not be empty", pack.scenes.isNotEmpty())

        val rootAssets = listOf(
            File("app/src/main/assets"),
            File("src/main/assets"),
            File("assets")
        ).firstOrNull { it.isDirectory }
        assertNotNull("Root assets directory should be found", rootAssets)

        for ((id, scene) in pack.scenes) {
            assertEquals(id, scene.id)
            assertTrue("name must not be blank for $id", scene.name.isNotBlank())
            assertTrue("backgroundAsset must not be blank for $id", scene.backgroundAsset.isNotBlank())

            val bgFile = File(rootAssets, scene.backgroundAsset)
            assertTrue("Background asset '${scene.backgroundAsset}' for scene '$id' must exist on disk", bgFile.exists())
        }

        assertTrue("Initial scene must exist in scenes", pack.scenes.containsKey(pack.manifest.initialSceneId))
    }

    @Test
    fun testCharactersAndNodesLoaded() {
        val pack = loadPack()
        assertTrue("Characters must not be empty", pack.characters.isNotEmpty())
        for ((id, char) in pack.characters) {
            assertEquals(id, char.id)
            assertTrue("Character name must not be blank for $id", char.name.isNotBlank())
        }

        assertTrue("Nodes must not be empty", pack.nodes.isNotEmpty())
        assertTrue("Initial node must exist in nodes", pack.nodes.containsKey(pack.manifest.initialNodeId))

        for ((id, node) in pack.nodes) {
            assertEquals(id, node.id)
            assertTrue("Node text must not be blank for $id", node.text.isNotBlank())
        }
    }
}
