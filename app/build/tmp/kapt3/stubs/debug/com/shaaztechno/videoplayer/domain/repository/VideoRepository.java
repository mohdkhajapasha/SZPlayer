package com.shaaztechno.videoplayer.domain.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000bH\u00a6@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u000f0\u000eH&J\u0018\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u000bH\u00a6@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u000f0\u000eH&J\u0018\u0010\u0014\u001a\u0004\u0018\u00010\u00052\u0006\u0010\n\u001a\u00020\u000bH\u00a6@\u00a2\u0006\u0002\u0010\fJ\u001c\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u000f0\u000e2\u0006\u0010\u0016\u001a\u00020\u0017H&J\u000e\u0010\u0018\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\u0019\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\bJ&\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001cH\u00a6@\u00a2\u0006\u0002\u0010\u001e\u00a8\u0006\u001f"}, d2 = {"Lcom/shaaztechno/videoplayer/domain/repository/VideoRepository;", "", "addVideo", "", "video", "Lcom/shaaztechno/videoplayer/domain/model/Video;", "(Lcom/shaaztechno/videoplayer/domain/model/Video;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearHistory", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteVideo", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllVideos", "Lkotlinx/coroutines/flow/Flow;", "", "getHistoryForVideo", "Lcom/shaaztechno/videoplayer/data/local/entity/HistoryEntity;", "videoId", "getPlaybackHistory", "getVideoById", "getVideosByType", "type", "Lcom/shaaztechno/videoplayer/domain/model/VideoType;", "refreshLocalVideos", "refreshOnlineCatalog", "updateHistory", "position", "", "duration", "(Lcom/shaaztechno/videoplayer/domain/model/Video;JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface VideoRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.shaaztechno.videoplayer.domain.model.Video>> getAllVideos();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.shaaztechno.videoplayer.domain.model.Video>> getVideosByType(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.VideoType type);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getVideoById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.shaaztechno.videoplayer.domain.model.Video> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshOnlineCatalog(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshLocalVideos(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object addVideo(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteVideo(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.shaaztechno.videoplayer.data.local.entity.HistoryEntity>> getPlaybackHistory();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getHistoryForVideo(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.shaaztechno.videoplayer.data.local.entity.HistoryEntity> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateHistory(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.domain.model.Video video, long position, long duration, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearHistory(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}