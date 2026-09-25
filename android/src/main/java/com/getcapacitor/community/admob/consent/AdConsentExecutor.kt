package com.getcapacitor.community.admob.consent

import android.app.Activity
import android.content.Context
import androidx.core.util.Supplier
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.models.Executor
import com.google.android.gms.common.util.BiConsumer
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

public class AdConsentExecutor(
    contextSupplier: Supplier<Context>,
    activitySupplier: Supplier<Activity?>,
    notifyListenersFunction: BiConsumer<String, JSObject>,
    pluginLogTag: String
) : Executor(contextSupplier, activitySupplier, notifyListenersFunction, pluginLogTag, "AdConsentExecutor") {
    private var consentInformation: ConsentInformation? = null

    @Suppress("UNUSED_PARAMETER")
    public fun requestConsentInfo(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>?) {
        try {
            val consentInformation = ensureConsentInfo()

            val paramsBuilder = ConsentRequestParameters.Builder()
            val debugSettingsBuilder = ConsentDebugSettings.Builder(contextSupplier.get())

            // The `!!` below keep the NullPointerException that a value of the wrong type caused, which the
            // catch at the end turns into a rejection.
            if (call.data.has("testDeviceIdentifiers")) {
                val devices = call.getArray("testDeviceIdentifiers")!!

                for (i in 0 until devices.length()) {
                    debugSettingsBuilder.addTestDeviceHashedId(devices.getString(i))
                }
            }

            if (call.data.has("debugGeography")) {
                debugSettingsBuilder.setDebugGeography(call.getInt("debugGeography")!!)
            }

            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())

            if (call.data.has("tagForUnderAgeOfConsent")) {
                paramsBuilder.setTagForUnderAgeOfConsent(call.getBoolean("tagForUnderAgeOfConsent")!!)
            }

            val consentRequestParameters = paramsBuilder.build()

            val activity = activitySupplier.get()
            if (activity == null) {
                call.reject("Trying to request consent info but the Activity is null")
                return
            }

            consentInformation.requestConsentInfoUpdate(
                activity,
                consentRequestParameters,
                {
                    val consentInfo = JSObject()
                    consentInfo.put("status", getConsentStatusString(consentInformation.consentStatus))
                    consentInfo.put("isConsentFormAvailable", consentInformation.isConsentFormAvailable)
                    consentInfo.put("canRequestAds", consentInformation.canRequestAds())
                    consentInfo.put("privacyOptionsRequirementStatus", consentInformation.privacyOptionsRequirementStatus.name)
                    call.resolve(consentInfo)
                },
                { formError -> call.reject(formError.message) }
            )
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    // Called on the main thread, where the forms are shown.
    @Suppress("UNUSED_PARAMETER")
    public fun showPrivacyOptionsForm(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>?) {
        try {
            val activity = activitySupplier.get()
            if (activity == null) {
                call.reject("Trying to show the privacy options form but the Activity is null")
                return
            }
            ensureConsentInfo()
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    call.reject("Error when show privacy form", formError.message)
                } else {
                    call.resolve()
                }
            }
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    // Called on the main thread, where the forms are shown.
    @Suppress("UNUSED_PARAMETER")
    public fun showConsentForm(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>?) {
        try {
            val activity = activitySupplier.get()
            if (activity == null) {
                call.reject("Trying to show the consent form but the Activity is null")
                return
            }

            val consentInformation = ensureConsentInfo()
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                if (formError != null) {
                    call.reject("Error when show consent form", formError.message)
                    return@loadAndShowConsentFormIfRequired
                }

                val consentFormInfo = JSObject()
                consentFormInfo.put("status", getConsentStatusString(consentInformation.consentStatus))
                consentFormInfo.put("canRequestAds", consentInformation.canRequestAds())
                consentFormInfo.put("privacyOptionsRequirementStatus", consentInformation.privacyOptionsRequirementStatus.name)
                call.resolve(consentFormInfo)
            }
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    public fun resetConsentInfo(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>?) {
        ensureConsentInfo().reset()
        call.resolve()
    }

    private fun getConsentStatusString(consentConstant: Int): String = when (consentConstant) {
        ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
        ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
        ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
        else -> "UNKNOWN"
    }

    private fun ensureConsentInfo(): ConsentInformation =
        consentInformation ?: UserMessagingPlatform.getConsentInformation(contextSupplier.get()).also { consentInformation = it }
}
