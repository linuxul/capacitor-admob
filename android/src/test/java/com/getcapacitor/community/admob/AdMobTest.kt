package com.getcapacitor.community.admob

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.Bridge
import com.getcapacitor.JSArray
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
import com.getcapacitor.community.admob.banner.BannerExecutor
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
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

            startSuspending { sut.initialize(pluginCallMock) }

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

            startSuspending { sut.initialize(pluginCallMock) }

            mobileAdsMockedStatic.verify({ MobileAds.setRequestConfiguration(argumentCaptor.capture()) }, times(1))
            assertEquals(testingDevices.toList<String>(), argumentCaptor.value.testDeviceIds)
        }

        @Test
        @DisplayName("Awaits the banner parent view group")
        fun bannerExecutorAwaitViewGroup() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(false)
            reportViewGroup(found = true)

            val result = startSuspending { sut.initialize(pluginCallMock) }

            verify(bannerExecutorMockedConstruction.constructed()[0]).awaitViewGroup(anyK())
            // Returning resolves the call.
            assertTrue(result?.isSuccess == true)
        }

        @Test
        @DisplayName("Is waiting while the banner parent view group is not there")
        fun waitsForTheViewGroup() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(false)

            assertNull(startSuspending { sut.initialize(pluginCallMock) })
        }

        @Test
        @DisplayName("Rejects when the banner parent view group never appears")
        fun viewGroupNeverAppears() {
            Mockito.`when`(pluginCallMock.getBoolean("initializeForTesting", false)).thenReturn(false)
            reportViewGroup(found = false)

            val error = startSuspending { sut.initialize(pluginCallMock) }?.exceptionOrNull() as? PluginException

            assertEquals("AdMob initialized, but the banner parent view never appeared", error?.message)
            assertNull(error?.code)
        }

        private fun reportViewGroup(found: Boolean) {
            doAnswer { invocation ->
                invocation.getArgument<(Boolean) -> Unit>(0)(found)
                null
            }
                .`when`(bannerExecutorMockedConstruction.constructed()[0])
                .awaitViewGroup(anyK())
        }
    }

    @Nested
    @DisplayName("Settings")
    inner class Settings {
        @Test
        @DisplayName("Rejects a missing muted value")
        fun missingMuted() {
            // Mockito answers false for a Boolean by default.
            Mockito.`when`(pluginCallMock.getBoolean("muted")).thenReturn(null)

            val error = assertThrows(PluginException::class.java) { sut.setApplicationMuted(pluginCallMock) }

            assertEquals("muted property cannot be null", error.message)
            assertNull(error.code)
        }

        @Test
        @DisplayName("Rejects a missing volume")
        fun missingVolume() {
            Mockito.`when`(pluginCallMock.getFloat("volume")).thenReturn(null)

            val error = assertThrows(PluginException::class.java) { sut.setApplicationVolume(pluginCallMock) }

            assertEquals("volume property cannot be null", error.message)
            assertNull(error.code)
        }
    }

    /** Starts [block] as a coroutine, as the bridge starts a suspend plugin method. Null while it is suspended. */
    private fun startSuspending(block: suspend () -> Unit): Result<Unit>? {
        var result: Result<Unit>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return result
    }
}
