package com.shaaztechno.videoplayer.presentation.library;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001:\u0001\"B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\f2\u0006\u0010\u0014\u001a\u00020\u0015J\u000e\u0010\u0016\u001a\u00020\u00122\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u0019\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\fJ\u0006\u0010\u001a\u001a\u00020\u0012J\u0016\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\f2\u0006\u0010\u001c\u001a\u00020\u0018J\u000e\u0010\u001d\u001a\u00020\u00122\u0006\u0010\u001e\u001a\u00020\u001fJ\u0006\u0010 \u001a\u00020\u0012J\b\u0010!\u001a\u00020\u0012H\u0002R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\t0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006#"}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/library/LibraryViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "playlistDao", "Lcom/shaaztechno/videoplayer/data/local/dao/PlaylistDao;", "(Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/local/dao/PlaylistDao;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/shaaztechno/videoplayer/presentation/library/LibraryUiState;", "allLocalVideos", "", "Lcom/shaaztechno/videoplayer/domain/model/Video;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "addVideoToPlaylist", "", "video", "playlistId", "", "createPlaylist", "name", "", "deleteVideo", "refresh", "renameVideo", "newTitle", "setSortOrder", "sortOrder", "Lcom/shaaztechno/videoplayer/presentation/library/SortOrder;", "toggleViewMode", "updateSortedVideos", "Factory", "app_debug"})
public final class LibraryViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.domain.repository.VideoRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.data.local.dao.PlaylistDao playlistDao = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.shaaztechno.videoplayer.presentation.library.LibraryUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.library.LibraryUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.shaaztechno.videoplayer.domain.model.Video> allLocalVideos;
    
    public LibraryViewModel(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.repository.VideoRepository repository, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.data.local.dao.PlaylistDao playlistDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.shaaztechno.videoplayer.presentation.library.LibraryUiState> getUiState() {
        return null;
    }
    
    public final void toggleViewMode() {
    }
    
    public final void setSortOrder(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.presentation.library.SortOrder sortOrder) {
    }
    
    private final void updateSortedVideos() {
    }
    
    public final void renameVideo(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video, @org.jetbrains.annotations.NotNull()
    java.lang.String newTitle) {
    }
    
    public final void deleteVideo(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video) {
    }
    
    public final void addVideoToPlaylist(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video, long playlistId) {
    }
    
    public final void createPlaylist(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void refresh() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J%\u0010\u0007\u001a\u0002H\b\"\b\b\u0000\u0010\b*\u00020\t2\f\u0010\n\u001a\b\u0012\u0004\u0012\u0002H\b0\u000bH\u0016\u00a2\u0006\u0002\u0010\fR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/shaaztechno/videoplayer/presentation/library/LibraryViewModel$Factory;", "Landroidx/lifecycle/ViewModelProvider$Factory;", "repository", "Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "playlistDao", "Lcom/shaaztechno/videoplayer/data/local/dao/PlaylistDao;", "(Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;Lcom/shaaztechno/videoplayer/data/local/dao/PlaylistDao;)V", "create", "T", "Landroidx/lifecycle/ViewModel;", "modelClass", "Ljava/lang/Class;", "(Ljava/lang/Class;)Landroidx/lifecycle/ViewModel;", "app_debug"})
    public static final class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.domain.repository.VideoRepository repository = null;
        @org.jetbrains.annotations.NotNull()
        private final com.shaaztechno.videoplayer.data.local.dao.PlaylistDao playlistDao = null;
        
        public Factory(@org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.domain.repository.VideoRepository repository, @org.jetbrains.annotations.NotNull()
        com.shaaztechno.videoplayer.data.local.dao.PlaylistDao playlistDao) {
            super();
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public <T extends androidx.lifecycle.ViewModel>T create(@org.jetbrains.annotations.NotNull()
        java.lang.Class<T> modelClass) {
            return null;
        }
    }
}