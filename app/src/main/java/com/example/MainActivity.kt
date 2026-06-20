package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceDashboardScreen
import com.example.ui.FinanceViewModel
import com.example.ui.theme.MyApplicationTheme
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: android.content.Context?) {
        if (newBase != null) {
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.fontScale = 1.0f
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.densityDpi = android.util.DisplayMetrics.DENSITY_DEVICE_STABLE
            }
            val newContext = newBase.createConfigurationContext(config)
            super.attachBaseContext(newContext)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Centralized Coil ImageLoader setup for lazy loading parameters, disk caching, and memory caching.
        try {
            val imageLoader = ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25) // Allocate up to 25% of available app memory specifically for profiles & images
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(this.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50L * 1024L * 1024L) // 50MB dedicated disk cache to persist profile avatars offline
                        .build()
                }
                .crossfade(true)
                .build()
            Coil.setImageLoader(imageLoader)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            val config = resources.configuration
            config.fontScale = 1.0f
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.densityDpi = android.util.DisplayMetrics.DENSITY_DEVICE_STABLE
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        enableEdgeToEdge()

        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val themeState by viewModel.selectedTheme.collectAsStateWithLifecycle()
            val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
            val darkTheme = when (themeState) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(
                selectedTheme = themeState,
                darkTheme = darkTheme,
                selectedLanguage = selectedLanguage
            ) {
                FinanceDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
