package com.shaaztechno.videoplayer.domain.usecase

import com.shaaztechno.videoplayer.domain.model.InstagramReel
import com.shaaztechno.videoplayer.domain.repository.InstagramRepository
import java.util.regex.Pattern

class GetInstagramReelUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(url: String): Result<InstagramReel> {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }
        
        // Regex to match instagram.com/reel/SHORTCODE or instagram.com/reels/SHORTCODE
        // Support instagr.am as well
        val reelPattern = Pattern.compile("(?:instagram\\.com|instagr\\.am)/(?:reel|reels)/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = reelPattern.matcher(trimmedUrl)
        
        if (!matcher.find()) {
            return Result.failure(IllegalArgumentException("Not a valid Instagram Reel URL. Please provide a link like https://www.instagram.com/reel/..."))
        }

        return repository.getReel(trimmedUrl)
    }
}
