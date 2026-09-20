package com.getcapacitor.community.admob.interstitial

import android.app.Activity
import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.anyK
import com.getcapacitor.community.admob.helpers.AdViewIdHelper
import com.getcapacitor.community.admob.helpers.RequestHelper
import com.getcapacitor.community.admob.models.AdOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.common.util.BiConsumer
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class AdInterstitialExecutorTest {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var mockedActivity: Activity

    @Mock
    lateinit var notifierMock: BiConsumer<String, JSObject>

    @Mock
    lateinit var interstitialAdCallbackAndListenersMock: InterstitialAdCallbackAndListeners

    private val logTag = "AdInterstitialExecutorTest Log Tag"

    lateinit var sut: AdInterstitialExecutor

    lateinit var runnableArgumentCaptor: ArgumentCaptor<Runnable>

    @BeforeEach
    fun beforeEach() {
        runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable::class.java)

        sut = AdInterstitialExecutor({ context }, { mockedActivity }, notifierMock, logTag, interstitialAdCallbackAndListenersMock)
    }

    @AfterEach
    fun afterEach() {
        reset(context, mockedActivity, notifierMock)
    }

    @Nested
    inner class PrepareInterstitial {
        @Mock
        lateinit var adOptionsFactoryMockedStatic: MockedStatic<AdOptions.AdOptionsFactory>

        @Mock
        lateinit var requestHelperMockedStatic: MockedStatic<RequestHelper>

        @Mock
        lateinit var adViewIdHelperMockedStatic: MockedStatic<AdViewIdHelper>

        @Mock
        lateinit var interstitialAdMockedStatic: MockedStatic<InterstitialAd>

        @Mock
        lateinit var interstitialAdLoadCallbackMock: InterstitialAdLoadCallback

        @Mock
        lateinit var pluginCallMock: PluginCall

        @Mock
        lateinit var adOptionsMock: AdOptions

        private val adRequestFromHelper = AdRequest.Builder().build()

        private val idFromViewHelper = "The Id From The View Helper"

        @BeforeEach
        fun beforeEach() {
            adOptionsFactoryMockedStatic
                .`when`<AdOptions> { AdOptions.AdOptionsFactory.createInterstitialOptions(pluginCallMock) }
                .thenReturn(adOptionsMock)
            requestHelperMockedStatic.`when`<AdRequest> { RequestHelper.createRequest(adOptionsMock) }.thenReturn(adRequestFromHelper)
            adViewIdHelperMockedStatic
                .`when`<String> { AdViewIdHelper.getFinalAdId(anyK(), anyK(), anyK(), anyK()) }
                .thenReturn(idFromViewHelper)
        }

        @AfterEach
        fun afterEach() {
            requestHelperMockedStatic.close()
            adOptionsFactoryMockedStatic.close()
            interstitialAdMockedStatic.close()
            adViewIdHelperMockedStatic.close()
        }

        @Test
        @DisplayName("creates the options with the correct adOption factory")
        fun createTheOptions() {
            sut.prepareInterstitial(pluginCallMock, notifierMock)

            adOptionsFactoryMockedStatic.verify { AdOptions.AdOptionsFactory.createInterstitialOptions(pluginCallMock) }
        }

        @Test
        @DisplayName("loads the ad with the id and request of the helper")
        fun usesIdHelper() {
            val idArgumentCaptor = ArgumentCaptor.forClass(String::class.java)
            val adRequestCaptor = ArgumentCaptor.forClass(AdRequest::class.java)

            sut.prepareInterstitial(pluginCallMock, notifierMock)
            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnable = runnableArgumentCaptor.value
            uiThreadRunnable.run()

            interstitialAdMockedStatic.verify {
                InterstitialAd.load(any(), idArgumentCaptor.capture(), adRequestCaptor.capture(), any())
            }

            assertEquals(idFromViewHelper, idArgumentCaptor.value)
            assertEquals(adRequestFromHelper, adRequestCaptor.value)
        }

        @Test
        @DisplayName("loads the ad with the InterstitialAdLoadCallback returned by the getInterstitialAdLoadCallback singleton")
        fun usesCallbackHelper() {
            Mockito.`when`(interstitialAdCallbackAndListenersMock.getInterstitialAdLoadCallback(pluginCallMock, notifierMock))
                .thenReturn(interstitialAdLoadCallbackMock)
            val callbackArgumentCaptor = ArgumentCaptor.forClass(InterstitialAdLoadCallback::class.java)

            sut.prepareInterstitial(pluginCallMock, notifierMock)
            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnable = runnableArgumentCaptor.value
            uiThreadRunnable.run()

            interstitialAdMockedStatic.verify { InterstitialAd.load(any(), any(), any(), callbackArgumentCaptor.capture()) }

            val callback = callbackArgumentCaptor.value

            assertEquals(interstitialAdLoadCallbackMock, callback)
        }
    }

    @Nested
    inner class ShowInterstitial {
        @Mock
        lateinit var pluginCallMock: PluginCall

        @BeforeEach
        fun beforeEach() {
            AdInterstitialExecutor.preparedAds.clear()
            AdInterstitialExecutor.lastPreparedAdId = null
        }

        @Test
        @DisplayName("Should reject the call when no Interstitial was prepared")
        fun rejectsWhenNoInterstitialWasLoaded() {
            val argumentCaptor = ArgumentCaptor.forClass(String::class.java)

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(pluginCallMock).reject(argumentCaptor.capture(), isNull(), isNull(), isNull())
            val resolvedError = argumentCaptor.value

            assertThat(resolvedError, containsString("not prepared"))
        }

        @Test
        @DisplayName("Should emit a Fail to show when no Interstitial was prepared")
        fun emitsFailToShowWhenNoInterstitialWasLoaded() {
            val argumentCaptor = ArgumentCaptor.forClass(JSObject::class.java)

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(notifierMock).accept(ArgumentMatchers.eq(InterstitialAdPluginPluginEvent.FailedToLoad), argumentCaptor.capture())

            val emittedError = argumentCaptor.value

            assertThat(emittedError.getString("message"), containsString("not prepared"))
        }

        @Test
        @DisplayName("Should not try to call show when no Interstitial was prepared")
        fun shouldNotCallShowWhenNotPrepared() {
            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(mockedActivity, times(0)).runOnUiThread(any())
        }

        @Test
        @DisplayName("Should call show when Interstitial was prepared")
        fun shouldCallShowWhenPrepared() {
            val mockedInterstitialAd = mock(InterstitialAd::class.java)
            AdInterstitialExecutor.preparedAds["test-ad-id"] = mockedInterstitialAd
            AdInterstitialExecutor.lastPreparedAdId = "test-ad-id"

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnable = runnableArgumentCaptor.value
            uiThreadRunnable.run()

            verify(pluginCallMock, times(0)).reject(any(), any(), any(), any())
            verify(mockedInterstitialAd).show(any())
        }

        @Test
        @DisplayName("Should show a specific ad when adId is provided")
        fun shouldShowSpecificAdWhenAdIdProvided() {
            val adOne = mock(InterstitialAd::class.java)
            val adTwo = mock(InterstitialAd::class.java)
            AdInterstitialExecutor.preparedAds["ad-unit-1"] = adOne
            AdInterstitialExecutor.preparedAds["ad-unit-2"] = adTwo
            AdInterstitialExecutor.lastPreparedAdId = "ad-unit-2"

            Mockito.`when`(pluginCallMock.getString("adId")).thenReturn("ad-unit-1")

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            runnableArgumentCaptor.value.run()

            verify(adOne).show(any())
            verify(adTwo, times(0)).show(any())
        }

        @Test
        @DisplayName("Should show the last prepared ad when no adId is provided")
        fun shouldShowLastPreparedWhenNoAdId() {
            val adOne = mock(InterstitialAd::class.java)
            val adTwo = mock(InterstitialAd::class.java)
            AdInterstitialExecutor.preparedAds["ad-unit-1"] = adOne
            AdInterstitialExecutor.preparedAds["ad-unit-2"] = adTwo
            AdInterstitialExecutor.lastPreparedAdId = "ad-unit-2"

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            runnableArgumentCaptor.value.run()

            verify(adTwo).show(any())
            verify(adOne, times(0)).show(any())
        }

        @Test
        @DisplayName("Should fall back to the remaining prepared ad after dismissal")
        fun shouldFallBackToRemainingAdAfterDismissal() {
            val adOne = mock(InterstitialAd::class.java)
            val adTwo = mock(InterstitialAd::class.java)
            AdInterstitialExecutor.preparedAds["ad-unit-1"] = adOne
            AdInterstitialExecutor.preparedAds["ad-unit-2"] = adTwo
            AdInterstitialExecutor.lastPreparedAdId = "ad-unit-2"

            sut.showInterstitial(pluginCallMock, notifierMock)
            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            runnableArgumentCaptor.value.run()

            val callback = ArgumentCaptor.forClass(FullScreenContentCallback::class.java)
            verify(adTwo).fullScreenContentCallback = callback.capture()
            callback.value.onAdDismissedFullScreenContent()

            reset(mockedActivity)
            sut.showInterstitial(pluginCallMock, notifierMock)
            verify(mockedActivity).runOnUiThread(runnableArgumentCaptor.capture())
            runnableArgumentCaptor.value.run()

            verify(adOne).show(any())
        }

        @Test
        @DisplayName("Should reject when requesting a non-existent adId")
        fun shouldRejectWhenAdIdNotFound() {
            val adOne = mock(InterstitialAd::class.java)
            AdInterstitialExecutor.preparedAds["ad-unit-1"] = adOne
            AdInterstitialExecutor.lastPreparedAdId = "ad-unit-1"

            Mockito.`when`(pluginCallMock.getString("adId")).thenReturn("non-existent")

            sut.showInterstitial(pluginCallMock, notifierMock)

            verify(pluginCallMock).reject(any(), isNull(), isNull(), isNull())
            verify(mockedActivity, times(0)).runOnUiThread(any())
        }
    }
}
