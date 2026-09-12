package com.shaaztechno.videoplayer.util

object Config {
    /**
     * The URL to the published Google Sheet CSV.
     * To get this:
     * 1. Create a Google Sheet with columns: id, title, description, videoUrl, thumbnailUrl, category, duration
     * 2. Go to File > Share > Publish to web.
     * 3. Select "Entire Document" and "Comma-separated values (.csv)".
     * 4. Copy the generated URL and paste it here.
     */
    const val GOOGLE_SHEET_CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vS7_n5-3QC0jNTQt4S1L6e41fPEpXtnjU5geRE2U6Rq0rUZMTeKnfYEofVvbkcAVrzxWaIOCMwgUSA9/pub?output=csv" // Replace with actual URL
    
    const val DEFAULT_SEEK_BACKWARD_MS = 10000L
    const val DEFAULT_SEEK_FORWARD_MS = 10000L
}
