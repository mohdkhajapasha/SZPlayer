package com.shaaztechno.videoplayer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.util.GoogleMobileAdsConsentManager

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val consentManager = remember { GoogleMobileAdsConsentManager.getInstance(context) }

    // UMP Consent Check: Do not request ads unless consent is gathered and ads can be requested
    if (!consentManager.canRequestAds) {
        return
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    // Calculate anchored adaptive banner size matching current orientation and width
    val adSize = remember(screenWidth) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = ctx.getString(R.string.banner_ad_unit_id)
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = {
                // Keep the current ad without triggering extra reload requests on recomposition
            },
            onRelease = { adView ->
                adView.destroy()
            }
        )
    }
}
