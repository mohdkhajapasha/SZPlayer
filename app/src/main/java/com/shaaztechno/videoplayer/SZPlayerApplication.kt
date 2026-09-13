package com.shaaztechno.videoplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.shaaztechno.videoplayer.data.local.SZPlayerDatabase
import com.shaaztechno.videoplayer.data.local.SettingsDataStore
import com.shaaztechno.videoplayer.data.remote.GoogleSheetParser
import com.shaaztechno.videoplayer.data.repository.VideoRepositoryImpl
import com.shaaztechno.videoplayer.data.downloader.VideoDownloader
import com.shaaztechno.videoplayer.data.remote.InstagramMediaExtractor
import com.shaaztechno.videoplayer.data.repository.InstagramRepositoryImpl
import com.shaaztechno.videoplayer.domain.usecase.GetInstagramReelUseCase
import okhttp3.OkHttpClient

class SZPlayerApplication : Application(), ImageLoaderFactory {
    
    val database by lazy { SZPlayerDatabase.getDatabase(this) }
    val settingsDataStore by lazy { SettingsDataStore(this) }
    
    private val httpClient by lazy { OkHttpClient() }
    private val googleSheetParser by lazy { GoogleSheetParser(httpClient) }
    
    val videoRepository by lazy { 
        VideoRepositoryImpl(
            videoDao = database.videoDao(),
            historyDao = database.historyDao(),
            googleSheetParser = googleSheetParser,
            context = this
        )
    }

    val videoDownloader by lazy {
        VideoDownloader(this, videoRepository)
    }

    val szDownloadManager by lazy {
        com.shaaztechno.videoplayer.data.downloader.SZDownloadManager.getInstance(this, videoRepository)
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
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
