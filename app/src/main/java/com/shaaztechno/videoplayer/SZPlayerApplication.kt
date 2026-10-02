package com.shaaztechno.videoplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.google.android.gms.ads.MobileAds
import com.shaaztechno.videoplayer.data.local.SZPlayerDatabase
import com.shaaztechno.videoplayer.data.local.SettingsDataStore
import com.shaaztechno.videoplayer.data.remote.GoogleSheetParser
import com.shaaztechno.videoplayer.data.repository.VideoRepositoryImpl
import com.shaaztechno.videoplayer.data.downloader.VideoDownloader
import com.shaaztechno.videoplayer.data.remote.InstagramMediaExtractor
import com.shaaztechno.videoplayer.data.repository.InstagramRepositoryImpl
import com.shaaztechno.videoplayer.data.repository.VideoStorageRepositoryImpl
import com.shaaztechno.videoplayer.domain.usecase.GetInstagramReelUseCase
import com.shaaztechno.videoplayer.domain.repository.VideoStorageRepository
import okhttp3.OkHttpClient

class SZPlayerApplication : Application(), ImageLoaderFactory {
    
    val database by lazy { SZPlayerDatabase.getDatabase(this) }
    val settingsDataStore by lazy { SettingsDataStore(this) }
    
    val httpClient by lazy { OkHttpClient() }
    private val googleSheetParser by lazy { GoogleSheetParser(httpClient) }
    
    val videoRepository by lazy { 
        VideoRepositoryImpl(
            videoDao = database.videoDao(),
            historyDao = database.historyDao(),
            googleSheetParser = googleSheetParser,
            context = this
        )
    }

    val videoStorageRepository: VideoStorageRepository by lazy {
        VideoStorageRepositoryImpl(this)
    }

    val videoDownloader by lazy {
        VideoDownloader(this, videoRepository)
    }

    val szDownloadManager by lazy {
        com.shaaztechno.videoplayer.data.downloader.SZDownloadManager.getInstance(this, videoRepository, videoStorageRepository, httpClient)
    }

    val videoUrlChecker by lazy {
        com.shaaztechno.videoplayer.data.remote.VideoUrlChecker()
    }

    val videoMediaResolver by lazy {
        com.shaaztechno.videoplayer.data.remote.VideoMediaResolver(this)
    }

    val videoUrlRepository by lazy {
        com.shaaztechno.videoplayer.data.repository.VideoUrlRepositoryImpl(videoUrlChecker, videoMediaResolver)
    }

    val checkVideoUrlUseCase by lazy {
        com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase(videoUrlRepository)
    }

    // Instagram Reel Feature DI
    private val instagramMediaExtractor by lazy { InstagramMediaExtractor(httpClient) }
    val instagramRepository by lazy { InstagramRepositoryImpl(instagramMediaExtractor) }
    val getInstagramReelUseCase by lazy { GetInstagramReelUseCase(instagramRepository) }

    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {}
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(com.shaaztechno.videoplayer.data.remote.VideoThumbnailFetcher.UriFactory(this@SZPlayerApplication, httpClient))
                add(com.shaaztechno.videoplayer.data.remote.VideoThumbnailFetcher.StringFactory(this@SZPlayerApplication, httpClient))
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .build()
    }
}
