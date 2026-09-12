package com.shaaztechno.videoplayer.data.downloader;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\rR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/shaaztechno/videoplayer/data/downloader/VideoDownloader;", "", "context", "Landroid/content/Context;", "repository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "(Landroid/content/Context;Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;)V", "downloadManager", "Landroid/app/DownloadManager;", "downloadVideo", "", "video", "Lcom/shaaztechno/videoplayer/domain/model/Video;", "(Lcom/shaaztechno/videoplayer/domain/model/Video;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class VideoDownloader {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.domain.repository.VideoRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.app.DownloadManager downloadManager = null;
    
    public VideoDownloader(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.repository.VideoRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object downloadVideo(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}