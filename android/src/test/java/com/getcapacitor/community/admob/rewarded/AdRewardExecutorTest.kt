package com.getcapacitor.community.admob.rewarded

import android.app.Activity
import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.common.util.BiConsumer
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
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
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class AdRewardExecutorTest {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var mockedActivity: Activity

    @Mock
    lateinit var notifierMock: BiConsumer<String, JSObject>

    private val logTag = "AdRewardExecutorTest Log Tag"

    lateinit var sut: AdRewardExecutor

    @BeforeEach
    fun beforeEach() {
        reset(context, mockedActivity, notifierMock)
        sut = AdRewardExecutor({ context }, { mockedActivity }, notifierMock, logTag)
    }

    @Nested
    inner class ShowRewardVideoAd {
        @Mock
        lateinit var pluginCallMock: PluginCall

        @BeforeEach
        fun beforeEach() {
            AdRewardExecutor.preparedAds.clear()
            AdRewardExecutor.lastPreparedAdId = null
        }

        @Test
        @DisplayName("Should reject the call when no Reward was prepared")
        fun rejectsWhenNoLoaded() {
            val argumentCaptor = ArgumentCaptor.forClass(String::class.java)

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(pluginCallMock).reject(argumentCaptor.capture(), isNull(), isNull(), isNull())
            val resolvedError = argumentCaptor.value

            assertThat(resolvedError, containsString("not prepared"))
        }

        @Test
        @DisplayName("Should emit a Fail to show when no Reward was prepared")
        fun emitsFailToShowWhenNoLoaded() {
            val argumentCaptor = ArgumentCaptor.forClass(JSObject::class.java)

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(notifierMock).accept(ArgumentMatchers.eq(RewardAdPluginEvents.FailedToLoad), argumentCaptor.capture())

            val emittedError = argumentCaptor.value

            assertThat(emittedError.getString("message"), containsString("not prepared"))
        }

        @Test
        @DisplayName("Should not resolve when no Reward was prepared")
        fun shouldNotResolveWhenNotPrepared() {
            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(pluginCallMock, times(0)).resolve(any())
        }

        @Test
        @DisplayName("Should call show when Reward was prepared")
        fun shouldCallShowWhenPrepared() {
            val mockedRewardedAd = mock(RewardedAd::class.java)
            AdRewardExecutor.preparedAds["test-ad-id"] = mockedRewardedAd
            AdRewardExecutor.lastPreparedAdId = "test-ad-id"

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(pluginCallMock, times(0)).reject(any(), any(), any(), any())
            verify(mockedRewardedAd).show(any(), any())
        }

        @Test
        @DisplayName("Should show a specific reward ad when adId is provided")
        fun shouldShowSpecificAdWhenAdIdProvided() {
            val adOne = mock(RewardedAd::class.java)
            val adTwo = mock(RewardedAd::class.java)
            AdRewardExecutor.preparedAds["reward-1"] = adOne
            AdRewardExecutor.preparedAds["reward-2"] = adTwo
            AdRewardExecutor.lastPreparedAdId = "reward-2"

            Mockito.`when`(pluginCallMock.getString("adId")).thenReturn("reward-1")

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(adOne).show(any(), any())
            verify(adTwo, times(0)).show(any(), any())
        }

        @Test
        @DisplayName("Should show the last prepared reward ad when no adId is provided")
        fun shouldShowLastPreparedWhenNoAdId() {
            val adOne = mock(RewardedAd::class.java)
            val adTwo = mock(RewardedAd::class.java)
            AdRewardExecutor.preparedAds["reward-1"] = adOne
            AdRewardExecutor.preparedAds["reward-2"] = adTwo
            AdRewardExecutor.lastPreparedAdId = "reward-2"

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(adTwo).show(any(), any())
            verify(adOne, times(0)).show(any(), any())
        }

        @Test
        @DisplayName("Should reject when requesting a non-existent reward adId")
        fun shouldRejectWhenAdIdNotFound() {
            val adOne = mock(RewardedAd::class.java)
            AdRewardExecutor.preparedAds["reward-1"] = adOne
            AdRewardExecutor.lastPreparedAdId = "reward-1"

            Mockito.`when`(pluginCallMock.getString("adId")).thenReturn("non-existent")

            sut.showRewardVideoAd(pluginCallMock, notifierMock)

            verify(pluginCallMock).reject(any(), isNull(), isNull(), isNull())
            verify(adOne, times(0)).show(any(), any())
        }
    }
}
