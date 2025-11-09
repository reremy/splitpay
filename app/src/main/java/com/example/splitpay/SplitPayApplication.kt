package com.example.splitpay

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.splitpay.logger.logI
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Custom Application class for SplitPay app.
 *
 * Configures:
 * - Firestore offline persistence (caches data locally)
 * - Coil image caching (optimized disk + memory cache)
 */
class SplitPayApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Enable Firestore offline persistence
        enableFirestoreOfflinePersistence()
    }

    /**
     * Enable Firestore offline data persistence.
     *
     * This significantly improves app performance by:
     * - Caching all Firestore data locally
     * - Allowing reads from cache when offline
     * - Reducing network calls for previously loaded data
     * - Automatically syncing changes when back online
     */
    private fun enableFirestoreOfflinePersistence() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enable offline persistence
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Allow unlimited cache
                .build()

            firestore.firestoreSettings = settings
            logI("Firestore offline persistence enabled with unlimited cache")
        } catch (e: Exception) {
            // Settings can only be applied before any Firestore operations
            logI("Firestore settings already initialized: ${e.message}")
        }
    }

    /**
     * Configure Coil ImageLoader with optimized caching.
     *
     * This improves image loading performance by:
     * - Using memory cache for instant display of recent images
     * - Using disk cache to avoid re-downloading images
     * - Configuring appropriate cache sizes
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of app's available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Always use cache, don't expire based on HTTP headers
            .build()
    }
}
