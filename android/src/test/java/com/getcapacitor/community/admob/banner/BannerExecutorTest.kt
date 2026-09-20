package com.getcapacitor.community.admob.banner

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.Display
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.anyK
import com.getcapacitor.community.admob.helpers.AdViewIdHelper
import com.getcapacitor.community.admob.helpers.RequestHelper
import com.getcapacitor.community.admob.models.AdOptions
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.common.util.BiConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class BannerExecutorTest {
    private val logTag = "BannerExecutorTest Log Tag"

    @Mock(lenient = true)
    lateinit var contextMock: Context

    @Mock(lenient = true)
    lateinit var activityMock: Activity

    @Mock(lenient = true)
    lateinit var windowMock: Window

    @Mock(lenient = true)
    lateinit var windowManagerMock: WindowManager

    @Mock(lenient = true)
    lateinit var displayMock: Display

    @Mock(lenient = true)
    lateinit var attributesMock: WindowManager.LayoutParams

    @Mock
    lateinit var notifierMock: BiConsumer<String, JSObject>

    val viewGroupMock: ViewGroup = mock(ViewGroup::class.java)

    lateinit var adViewMockedConstruction: MockedConstruction<AdView>

    lateinit var sut: BannerExecutor

    lateinit var adSizeStaticMock: MockedStatic<AdSize>

    @BeforeEach
    fun beforeEach() {
        adSizeStaticMock = Mockito.mockStatic(AdSize::class.java)
        reset(contextMock, activityMock, notifierMock, windowMock, windowManagerMock, displayMock, attributesMock)
        adViewMockedConstruction = Mockito.mockConstruction(AdView::class.java)

        Mockito.`when`(activityMock.windowManager).thenReturn(windowManagerMock)
        Mockito.`when`(activityMock.window).thenReturn(windowMock)

        Mockito.`when`(windowMock.decorView).thenReturn(viewGroupMock)

        @Suppress("DEPRECATION")
        Mockito.`when`(windowManagerMock.defaultDisplay).thenReturn(displayMock)
        Mockito.`when`(windowMock.attributes).thenReturn(attributesMock)

        Mockito.`when`(activityMock.findViewById<ViewGroup>(anyInt())).thenReturn(viewGroupMock)
        Mockito.`when`(viewGroupMock.getChildAt(anyInt())).thenReturn(viewGroupMock)

        sut = BannerExecutor({ contextMock }, { activityMock }, notifierMock, logTag)
    }

    @AfterEach
    fun afterEach() {
        adViewMockedConstruction.close()
        adSizeStaticMock.close()
    }

    @Test
    @DisplayName("#awaitViewGroup gets the reference of the viewGroup where the banner ad will go")
    fun awaitViewGroup() {
        sut.awaitViewGroup {}
        verify(viewGroupMock).getChildAt(0)
    }

    @Nested
    @DisplayName("Show Banner")
    inner class ShowBanner {
        @Mock
        lateinit var relativeLayoutMockedConstruction: MockedConstruction<RelativeLayout>

        @Mock
        lateinit var layoutParamsMockedConstruction: MockedConstruction<CoordinatorLayout.LayoutParams>

        @Mock
        lateinit var adOptionsFactoryMockedStatic: MockedStatic<AdOptions.AdOptionsFactory>

        @Mock
        lateinit var requestHelperMockedStatic: MockedStatic<RequestHelper>

        @Mock
        lateinit var adViewIdHelperMockedStatic: MockedStatic<AdViewIdHelper>

        @Mock
        lateinit var resourcesMock: Resources

        @Mock
        lateinit var displayMetricsMock: DisplayMetrics

        lateinit var runnableArgumentCaptor: ArgumentCaptor<Runnable>
        lateinit var adOptionsMockForTesting: AdOptions

        @BeforeEach
        fun beforeEach() {
            reset(resourcesMock, displayMetricsMock)
            runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable::class.java)
            displayMetricsMock.density = 1f

            adOptionsMockForTesting = AdOptions.TesterAdOptionsBuilder().build()

            adOptionsFactoryMockedStatic
                .`when`<AdOptions> { AdOptions.AdOptionsFactory.createBannerOptions(anyK()) }
                .thenReturn(adOptionsMockForTesting)

            Mockito.`when`(contextMock.resources).thenReturn(resourcesMock)
            Mockito.`when`(resourcesMock.displayMetrics).thenReturn(displayMetricsMock)

            sut.awaitViewGroup {}
        }

        @AfterEach
        fun afterEach() {
            adOptionsFactoryMockedStatic.close()
            relativeLayoutMockedConstruction.close()
            layoutParamsMockedConstruction.close()
            requestHelperMockedStatic.close()
            adViewIdHelperMockedStatic.close()
        }

        @Test
        @DisplayName("Banner constructs the request using the RequestHelper")
        fun showBannerUsesRequestHelper() {
            val pluginCallMock = mock(PluginCall::class.java)

            sut.showBanner(pluginCallMock)
            verify(activityMock).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnable = runnableArgumentCaptor.value
            uiThreadRunnable.run()

            requestHelperMockedStatic.verify { RequestHelper.createRequest(adOptionsMockForTesting) }
        }

        @Test
        @DisplayName("Updates the banner if more than one show request is done")
        fun showBanner() {
            val pluginCallMock = mock(PluginCall::class.java)

            sut.showBanner(pluginCallMock)
            sut.showBanner(pluginCallMock)

            verify(activityMock, atLeast(1)).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnableSecondCall = runnableArgumentCaptor.allValues
            uiThreadRunnableSecondCall.forEach(Runnable::run)

            val adViewMocked = adViewMockedConstruction.constructed()[0]
            verify(adViewMocked, times(2)).loadAd(any())
        }
    }

    @Nested
    @DisplayName("Hide Banner")
    inner class HideBanner {
        @Mock
        lateinit var relativeLayoutMockedConstruction: MockedConstruction<RelativeLayout>

        @Mock
        lateinit var layoutParamsMockedConstruction: MockedConstruction<CoordinatorLayout.LayoutParams>

        @Mock(lenient = true)
        lateinit var adOptionsFactoryMockedStatic: MockedStatic<AdOptions.AdOptionsFactory>

        @Mock(lenient = true)
        lateinit var requestHelperMockedStatic: MockedStatic<RequestHelper>

        @Mock(lenient = true)
        lateinit var adViewIdHelperMockedStatic: MockedStatic<AdViewIdHelper>

        @Mock(lenient = true)
        lateinit var resourcesMock: Resources

        @Mock(lenient = true)
        lateinit var displayMetricsMock: DisplayMetrics

        lateinit var runnableArgumentCaptor: ArgumentCaptor<Runnable>
        lateinit var adOptionsMockForTesting: AdOptions

        @BeforeEach
        fun beforeEach() {
            reset(resourcesMock, displayMetricsMock)
            runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable::class.java)
            displayMetricsMock.density = 1f

            adOptionsMockForTesting = AdOptions.TesterAdOptionsBuilder().build()

            adOptionsFactoryMockedStatic
                .`when`<AdOptions> { AdOptions.AdOptionsFactory.createBannerOptions(anyK()) }
                .thenReturn(adOptionsMockForTesting)

            Mockito.`when`(contextMock.resources).thenReturn(resourcesMock)
            Mockito.`when`(resourcesMock.displayMetrics).thenReturn(displayMetricsMock)

            sut.awaitViewGroup {}
        }

        @AfterEach
        fun afterEach() {
            adOptionsFactoryMockedStatic.close()
            relativeLayoutMockedConstruction.close()
            layoutParamsMockedConstruction.close()
            requestHelperMockedStatic.close()
            adViewIdHelperMockedStatic.close()
        }

        @Test
        @DisplayName("Hides the banner if it exist")
        fun hideBanner() {
            val pluginCallMock = mock(PluginCall::class.java)

            sut.showBanner(pluginCallMock)
            sut.hideBanner(pluginCallMock)

            verify(activityMock, atLeast(1)).runOnUiThread(runnableArgumentCaptor.capture())
            val uiThreadRunnableSecondCall = runnableArgumentCaptor.allValues
            uiThreadRunnableSecondCall.forEach(Runnable::run)

            val adViewMocked = adViewMockedConstruction.constructed()[0]
            verify(adViewMocked, times(1)).pause()
        }

        @Test
        @DisplayName("If not banner exist, return an error")
        fun hideBannerWithoutExistentBanner() {
            val pluginCallMock = mock(PluginCall::class.java)
            assertEquals(0, adViewMockedConstruction.constructed().size) // Correct environment

            sut.hideBanner(pluginCallMock)

            verify(activityMock, times(0)).runOnUiThread(runnableArgumentCaptor.capture()) // No Ui Calls
            verify(pluginCallMock, times(1)).reject(any(), isNull(), isNull(), isNull())
        }
    }
}
