package com.getcapacitor.community.admob

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.Bridge
import com.getcapacitor.JSArray
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.banner.BannerExecutor
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.lenient
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AdMobTest {
    @Mock
    lateinit var mockedContext: Context

    @Mock
    lateinit var mockedActivity: AppCompatActivity

    @Mock
    lateinit var mockedBridge: Bridge

    @Mock
    lateinit var pluginCallMock: PluginCall

    @Mock
    lateinit var bannerExecutorMockedConstruction: MockedConstruction<BannerExecutor>

    lateinit var sut: AdMob

    @BeforeEach
    fun beforeEach() {
        reset(pluginCallMock, mockedContext)

        // The plugin reads its context and activity from the bridge; they can no longer be overridden.
        lenient().`when`(mockedBridge.context).thenReturn(mockedContext)
        lenient().`when`(mockedBridge.activity).thenReturn(mockedActivity)

        sut = AdMob()
        sut.bridge = mockedBridge
    }

    @AfterEach
    fun afterEach() {
        bannerExecutorMockedConstruction.close()
    }

    @Nested
    @DisplayName("Initialize()")
    inner class Initialize {
        lateinit var mobileAdsMockedStatic: MockedStatic<MobileAds>
        lateinit var testingDevices: JSArray

        lateinit var argumentCaptor: ArgumentCaptor<RequestConfiguration>

        @BeforeEach
        fun beforeEachInitializeTest() {
            mobileAdsMockedStatic = Mockito.mockStatic(MobileAds::class.java)
            argumentCaptor = ArgumentCaptor.forClass(RequestConfiguration::class.java)
        }

        @AfterEach
        fun afterEachInitializeTest() {
            mobileAdsMockedStatic.close()
        }

        @Test
        @DisplayName("If we initialize in not testing mode, then set the testing devices to an empty list")
        fun emptyTestingDevices() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(false)
            assertEquals(argumentCaptor.allValues.size, 0) // Correct env

            sut.initialize(pluginCallMock)

            mobileAdsMockedStatic.verify({ MobileAds.setRequestConfiguration(argumentCaptor.capture()) }, times(1))
            assertEquals(0, argumentCaptor.value.testDeviceIds.size)
        }

        @Test
        @DisplayName("Register Testing Devices if in testing Mode")
        fun registerTestingDevices() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(true)
            testingDevices = JSArray()
            testingDevices.put("One")
            testingDevices.put("Two")
            Mockito.`when`(pluginCallMock.getArray("testingDevices", AdMob.EMPTY_TESTING_DEVICES)).thenReturn(testingDevices)
            assertEquals(argumentCaptor.allValues.size, 0) // Correct env

            sut.initialize(pluginCallMock)

            mobileAdsMockedStatic.verify({ MobileAds.setRequestConfiguration(argumentCaptor.capture()) }, times(1))
            assertEquals(testingDevices.toList<String>(), argumentCaptor.value.testDeviceIds)
        }

        @Test
        @DisplayName("Awaits the banner parent view group")
        fun bannerExecutorAwaitViewGroup() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(false)
            doAnswer { invocation ->
                invocation.getArgument<Runnable>(0).run()
                null
            }
                .`when`(mockedActivity)
                .runOnUiThread(any(Runnable::class.java))

            val bannerExecutor = bannerExecutorMockedConstruction.constructed()[0]
            doAnswer { invocation ->
                invocation.getArgument<(Boolean) -> Unit>(0)(true)
                null
            }
                .`when`(bannerExecutor)
                .awaitViewGroup(anyK())

            sut.initialize(pluginCallMock)

            verify(bannerExecutor).awaitViewGroup(anyK())
        }
    }
}
