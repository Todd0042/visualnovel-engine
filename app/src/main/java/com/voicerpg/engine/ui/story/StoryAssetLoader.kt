package com.voicerpg.engine.ui.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Loads and caches visual novel backgrounds and character sprites from assets
 * with a strict LRU memory ceiling (holding a maximum of 10 active textures)
 * and automatic downsampling to prevent OutOfMemory (OOM) crashes across 80+ scenes.
 */
object StoryAssetLoader {

    private const val MAX_CACHE_ENTRIES = 10
    private const val MAX_TEXTURE_DIMENSION = 1920

    // Strict LRU memory cache holding maximum 10 active textures
    private val bitmapCache = object : LruCache<String, ImageBitmap>(MAX_CACHE_ENTRIES) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: ImageBitmap?, newValue: ImageBitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted && oldValue != null) {
                try {
                    val androidBitmap = oldValue.asAndroidBitmap()
                    if (!androidBitmap.isRecycled) {
                        androidBitmap.recycle()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    // Set of known missing asset paths to avoid repeated disk reads
    private val missingAssets = hashSetOf<String>()

    /**
     * Loads a bitmap asset from the Android assets directory with downsampling and LRU caching.
     */
    fun loadBitmap(context: Context, assetPath: String): ImageBitmap? {
        if (assetPath.isBlank()) return null
        val cached = bitmapCache.get(assetPath)
        if (cached != null) return cached
        if (missingAssets.contains(assetPath)) return null

        val bitmap = tryLoadBitmap(context, assetPath)
        if (bitmap != null) {
            bitmapCache.put(assetPath, bitmap)
        } else {
            missingAssets.add(assetPath)
        }
        return bitmap
    }

    /**
     * Called during scene transitions to clear or evict oldest textures and prevent OOM.
     */
    fun onSceneTransition() {
        if (bitmapCache.size() > 5) {
            bitmapCache.trimToSize(4)
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
        missingAssets.clear()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun tryLoadBitmap(
        context: Context,
        assetPath: String
    ): ImageBitmap? = try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, MAX_TEXTURE_DIMENSION, MAX_TEXTURE_DIMENSION)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}
