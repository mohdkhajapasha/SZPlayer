package com.shaaztechno.videoplayer.presentation.downloads;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001:\u0001\u0017B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0015\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0018"}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/downloads/DownloadsViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "downloadManager", "Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "(Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/shaaztechno/videoplayer/presentation/downloads/DownloadsUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "cancelDownload", "", "id", "", "deleteDownload", "video", "Lcom/shaaztechno/videoplayer/domain/model/Video;", "pauseDownload", "resumeDownload", "Factory", "app_debug"})
public final class DownloadsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.domain.repository.VideoRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.shaaztechno.videoplayer.presentation.downloads.DownloadsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.downloads.DownloadsUiState> uiState = null;
    
    public DownloadsViewModel(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.repository.VideoRepository repository, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.downloads.DownloadsUiState> getUiState() {
        return null;
    }
    
    public final void pauseDownload(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void resumeDownload(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void cancelDownload(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void deleteDownload(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J%\u0010\u0007\u001a\u0002H\b\"\b\b\u0000\u0010\b*\u00020\t2\f\u0010\n\u001a\b\u0012\u0004\u0012\u0002H\b0\u000bH\u0016\u00a2\u0006\u0002\u0010\fR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/downloads/DownloadsViewModel$Factory;", "Landroidx/lifecycle/ViewModelProvider$Factory;", "repository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "downloadManager", "Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "(Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;)V", "create", "T", "Landroidx/lifecycle/ViewModel;", "modelClass", "Ljava/lang/Class;", "(Ljava/lang/Class;)Landroidx/lifecycle/ViewModel;", "app_debug"})
    public static final class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.domain.repository.VideoRepository repository = null;
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager = null;
        
        public Factory(@org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.domain.repository.VideoRepository repository, @org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager) {
            super();
        }
        
        @java.lang.Override()
        @kotlin.Suppress(names = {"UNCHECKED_CAST"})
        @org.jetbrains.annotations.NotNull()
        public <T extends androidx.lifecycle.ViewModel>T create(@org.jetbrains.annotations.NotNull()
        java.lang.Class<T> modelClass) {
            return null;
        }
    }
}