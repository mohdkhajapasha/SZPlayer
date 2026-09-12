package com.shaaztechno.videoplayer.presentation.videourl;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001!B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0014\u001a\u00020\u0015J\u0014\u0010\u0016\u001a\u00020\u00152\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00150\u0018J\u0006\u0010\u0019\u001a\u00020\u0015J\u000e\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u001b\u001a\u00020\rJ\u000e\u0010\u001c\u001a\u00020\u00152\u0006\u0010\u001d\u001a\u00020\rJ\u001a\u0010\u001e\u001a\u00020\u00152\u0012\u0010\u001f\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u00150 R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0011R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\""}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/videourl/VideoUrlViewModel;", "Landroidx/lifecycle/ViewModel;", "checkVideoUrlUseCase", "Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;", "videoRepository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "downloadManager", "Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "(Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/shaaztechno/videoplayer/presentation/videourl/VideoUrlUiState;", "_urlInput", "", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "urlInput", "getUrlInput", "checkUrl", "", "downloadVideo", "onNavigateToDownloads", "Lkotlin/Function0;", "onClearUrl", "onPasteUrl", "pastedText", "onUrlChanged", "newUrl", "playVideo", "onNavigateToPlayer", "Lkotlin/Function1;", "Factory", "app_debug"})
public final class VideoUrlViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase checkVideoUrlUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.domain.repository.VideoRepository videoRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _urlInput = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> urlInput = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.shaaztechno.videoplayer.presentation.videourl.VideoUrlUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.videourl.VideoUrlUiState> uiState = null;
    
    public VideoUrlViewModel(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase checkVideoUrlUseCase, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.repository.VideoRepository videoRepository, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getUrlInput() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.videourl.VideoUrlUiState> getUiState() {
        return null;
    }
    
    public final void onUrlChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String newUrl) {
    }
    
    public final void onClearUrl() {
    }
    
    public final void onPasteUrl(@org.jetbrains.annotations.NotNull()
    java.lang.String pastedText) {
    }
    
    public final void checkUrl() {
    }
    
    public final void playVideo(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onNavigateToPlayer) {
    }
    
    public final void downloadVideo(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToDownloads) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ%\u0010\t\u001a\u0002H\n\"\b\b\u0000\u0010\n*\u00020\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u0002H\n0\rH\u0016\u00a2\u0006\u0002\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/videourl/VideoUrlViewModel$Factory;", "Landroidx/lifecycle/ViewModelProvider$Factory;", "checkVideoUrlUseCase", "Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;", "videoRepository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "downloadManager", "Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;", "(Lcom/shaaztechno/videoplayer/domain/usecase/CheckVideoUrlUseCase;Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/downloader/SZDownloadManager;)V", "create", "T", "Landroidx/lifecycle/ViewModel;", "modelClass", "Ljava/lang/Class;", "(Ljava/lang/Class;)Landroidx/lifecycle/ViewModel;", "app_debug"})
    public static final class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase checkVideoUrlUseCase = null;
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.domain.repository.VideoRepository videoRepository = null;
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.data.downloader.SZDownloadManager downloadManager = null;
        
        public Factory(@org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.domain.usecase.CheckVideoUrlUseCase checkVideoUrlUseCase, @org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.domain.repository.VideoRepository videoRepository, @org.jetbrains.annotations.NotNull()
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