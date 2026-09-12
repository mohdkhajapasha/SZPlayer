package com.shaaztechno.videoplayer.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J \u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0006H\u0002J4\u0010\n\u001a\u0012\u0012\u0006\u0012\u0004\u0018\u00010\u0006\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u000b2\u0006\u0010\f\u001a\u00020\u00062\b\u0010\r\u001a\u0004\u0018\u00010\u00062\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0002J\u0017\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\f\u001a\u00020\u0006H\u0002\u00a2\u0006\u0002\u0010\u0012J)\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u00142\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0016\u001a\u00020\u0017\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0018\u0010\u0019R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001a"}, d2 = {"Lcom/shaaztechno/videoplayer/data/remote/VideoMediaResolver;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "deriveTitle", "", "originalUrl", "effectiveUrl", "mediaType", "detectMediaType", "Lkotlin/Pair;", "url", "contentType", "initialBytes", "", "fetchDuration", "", "(Ljava/lang/String;)Ljava/lang/Long;", "resolveMediaInfo", "Lkotlin/Result;", "Lcom/shaaztechno/videoplayer/domain/model/VideoMediaInfo;", "checkResponse", "Lcom/shaaztechno/videoplayer/data/remote/UrlCheckResponse;", "resolveMediaInfo-gIAlu-s", "(Ljava/lang/String;Lcom/shaaztechno/videoplayer/data/remote/UrlCheckResponse;)Ljava/lang/Object;", "app_debug"})
public final class VideoMediaResolver {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    
    public VideoMediaResolver(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final kotlin.Pair<java.lang.String, java.lang.String> detectMediaType(java.lang.String url, java.lang.String contentType, byte[] initialBytes) {
        return null;
    }
    
    private final java.lang.String deriveTitle(java.lang.String originalUrl, java.lang.String effectiveUrl, java.lang.String mediaType) {
        return null;
    }
    
    private final java.lang.Long fetchDuration(java.lang.String url) {
        return null;
    }
}