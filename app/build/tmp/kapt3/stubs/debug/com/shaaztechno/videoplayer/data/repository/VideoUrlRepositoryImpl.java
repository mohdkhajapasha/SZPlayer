package com.shaaztechno.videoplayer.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\f\u0010\rR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u000e"}, d2 = {"Lcom/shaaztechno/videoplayer/data/repository/VideoUrlRepositoryImpl;", "Lcom/shaaztechno/videoplayer/domain/repository/VideoUrlRepository;", "urlChecker", "Lcom/shaaztechno/videoplayer/data/remote/VideoUrlChecker;", "mediaResolver", "Lcom/shaaztechno/videoplayer/data/remote/VideoMediaResolver;", "(Lcom/shaaztechno/videoplayer/data/remote/VideoUrlChecker;Lcom/shaaztechno/videoplayer/data/remote/VideoMediaResolver;)V", "checkUrl", "Lkotlin/Result;", "Lcom/shaaztechno/videoplayer/domain/model/VideoMediaInfo;", "url", "", "checkUrl-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class VideoUrlRepositoryImpl implements com.shaaztechno.videoplayer.domain.repository.VideoUrlRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.data.remote.VideoUrlChecker urlChecker = null;
    @org.jetbrains.annotations.NotNull()
    private final com.shaaztechno.videoplayer.data.remote.VideoMediaResolver mediaResolver = null;
    
    public VideoUrlRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.data.remote.VideoUrlChecker urlChecker, @org.jetbrains.annotations.NotNull()
    com.shaaztechno.videoplayer.data.remote.VideoMediaResolver mediaResolver) {
        super();
    }
}