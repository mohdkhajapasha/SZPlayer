package com.shaaztechno.videoplayer;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000t\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\b\u0010<\u001a\u00020=H\u0016J\b\u0010>\u001a\u00020?H\u0016R\u001b\u0010\u0004\u001a\u00020\u00058FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\t\u001a\u0004\b\f\u0010\rR\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\t\u001a\u0004\b\u0011\u0010\u0012R\u001b\u0010\u0014\u001a\u00020\u00158BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0018\u0010\t\u001a\u0004\b\u0016\u0010\u0017R\u001b\u0010\u0019\u001a\u00020\u001a8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001d\u0010\t\u001a\u0004\b\u001b\u0010\u001cR\u001b\u0010\u001e\u001a\u00020\u001f8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\"\u0010\t\u001a\u0004\b \u0010!R\u001b\u0010#\u001a\u00020$8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\'\u0010\t\u001a\u0004\b%\u0010&R\u001b\u0010(\u001a\u00020)8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b,\u0010\t\u001a\u0004\b*\u0010+R\u001b\u0010-\u001a\u00020.8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b1\u0010\t\u001a\u0004\b/\u00100R\u001b\u00102\u001a\u0002038FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b6\u0010\t\u001a\u0004\b4\u00105R\u001b\u00107\u001a\u0002088FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b;\u0010\t\u001a\u0004\b9\u0010:\u00a8\u0006@"}, d2 = {"Lcom/shaaztechno/videoplayer/SZPlayerApplication;", "Landroid/app/Application;", "Lcoil/ImageLoaderFactory;", "()V", "checkVideoUrlUseCase", "Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;", "getCheckVideoUrlUseCase", "()Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;", "checkVideoUrlUseCase$delegate", "Lkotlin/Lazy;", "database", "Lcom/shaaztechno/videoplayer/data/local/SZPlayerDatabase;", "getDatabase", "()Lcom/shaaztechno/videoplayer/data/local/SZPlayerDatabase;", "database$delegate", "googleSheetParser", "Lcom/shaaztechno/videoplayer/data/remote/GoogleSheetParser;", "getGoogleSheetParser", "()Lcom/shaaztechno/videoplayer/data/remote/GoogleSheetParser;", "googleSheetParser$delegate", "httpClient", "Lokhttp3/OkHttpClient;", "getHttpClient", "()Lokhttp3/OkHttpClient;", "httpClient$delegate", "settingsDataStore", "Lcom/shaaztechno/videoplayer/data/local/SettingsDataStore;", "getSettingsDataStore", "()Lcom/shaaztechno/videoplayer/data/local/SettingsDataStore;", "settingsDataStore$delegate", "szDownloadManager", "Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "getSzDownloadManager", "()Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "szDownloadManager$delegate", "videoDownloader", "Lcom/shaaztechno/videoplayer/data/downloader/VideoDownloader;", "getVideoDownloader", "()Lcom/shaaztechno/videoplayer/data/downloader/VideoDownloader;", "videoDownloader$delegate", "videoMediaResolver", "Lcom/shaaztechno/videoplayer/data/remote/VideoMediaResolver;", "getVideoMediaResolver", "()Lcom/shaaztechno/videoplayer/data/remote/VideoMediaResolver;", "videoMediaResolver$delegate", "videoRepository", "Lcom/shaaztechno/videoplayer/data/repository/VideoRepositoryImpl;", "getVideoRepository", "()Lcom/shaaztechno/videoplayer/data/repository/VideoRepositoryImpl;", "videoRepository$delegate", "videoUrlChecker", "Lcom/shaaztechno/videoplayer/data/remote/VideoUrlChecker;", "getVideoUrlChecker", "()Lcom/shaaztechno/videoplayer/data/remote/VideoUrlChecker;", "videoUrlChecker$delegate", "videoUrlRepository", "Lcom/shaaztechno/videoplayer/data/repository/VideoUrlRepositoryImpl;", "getVideoUrlRepository", "()Lcom/shaaztechno/videoplayer/data/repository/VideoUrlRepositoryImpl;", "videoUrlRepository$delegate", "newImageLoader", "Lcoil/ImageLoader;", "onCreate", "", "app_debug"})
public final class SZPlayerApplication extends android.app.Application implements coil.ImageLoaderFactory {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy database$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy settingsDataStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy httpClient$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy googleSheetParser$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy videoRepository$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy videoDownloader$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy szDownloadManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy videoUrlChecker$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy videoMediaResolver$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy videoUrlRepository$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy checkVideoUrlUseCase$delegate = null;
    
    public SZPlayerApplication() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.local.SZPlayerDatabase getDatabase() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.local.SettingsDataStore getSettingsDataStore() {
        return null;
    }
    
    private final okhttp3.OkHttpClient getHttpClient() {
        return null;
    }
    
    private final com.shaaztechno.videoplayer.data.remote.GoogleSheetParser getGoogleSheetParser() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.repository.VideoRepositoryImpl getVideoRepository() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.downloader.VideoDownloader getVideoDownloader() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.downloader.SZDownloadManager getSzDownloadManager() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.remote.VideoUrlChecker getVideoUrlChecker() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.remote.VideoMediaResolver getVideoMediaResolver() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.data.repository.VideoUrlRepositoryImpl getVideoUrlRepository() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase getCheckVideoUrlUseCase() {
        return null;
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public coil.ImageLoader newImageLoader() {
        return null;
    }
}