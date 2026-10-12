package com.shaaztechno.videoplayer.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Helper manager for Google Mobile Ads User Messaging Platform (UMP) SDK.
 * Handles consent gathering (GDPR / IAB TCF 2.2) before requesting AdMob ads.
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * Determines whether the app is allowed to request ads based on user consent status.
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * Determines whether the privacy options form is required (e.g. for EEA/UK users to alter choices).
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Requests consent info update and presents the consent form if required by law.
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: (consentError: Exception?) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode}: ${formError.message}")
                        onConsentGatheringCompleteListener(Exception(formError.message))
                    } else {
                        onConsentGatheringCompleteListener(null)
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent request error: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                onConsentGatheringCompleteListener(Exception(requestConsentError.message))
            }
        )
    }

    /**
     * Shows the privacy options form if the user wishes to change their consent preferences.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: (formError: Exception?) -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.errorCode}: ${formError.message}")
                onConsentFormDismissedListener(Exception(formError.message))
            } else {
                onConsentFormDismissedListener(null)
            }
        }
    }

    companion object {
        private const val TAG = "ConsentManager"

        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager =
            instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
