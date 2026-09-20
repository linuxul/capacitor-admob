package com.getcapacitor.community.admob.helpers

import android.content.Context
import android.util.Log
import com.getcapacitor.community.admob.models.AdOptions
import com.google.android.gms.ads.AdRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class AdViewIdHelperTest {
    @Mock
    lateinit var contextMock: Context

    lateinit var logMockedStatic: MockedStatic<Log>

    @BeforeEach
    fun setUp() {
        logMockedStatic = Mockito.mockStatic(Log::class.java)
    }

    @AfterEach
    fun tearDown() {
        logMockedStatic.close()
    }

    @Nested
    @DisplayName("#getFinalAdId()")
    inner class GeFinalAdId {
        @Test
        @DisplayName("Returns the real adId if the adOptions is not for testing")
        fun notAdOptionsForTesting() {
            val adOptions = AdOptions.TesterAdOptionsBuilder().setIsTesting(false).build()

            val returnedId = AdViewIdHelper.getFinalAdId(adOptions, mock(AdRequest::class.java), "test", contextMock)

            assertEquals(adOptions.adId, returnedId)
        }

        @Test
        @DisplayName("Returns the real adId if the adOptions is for testing but we are on a registered testing device")
        fun testingWithATestingDevice() {
            val adOptions = AdOptions.TesterAdOptionsBuilder().setIsTesting(true).build()
            val adRequest = mock(AdRequest::class.java)
            Mockito.`when`(adRequest.isTestDevice(any())).thenReturn(true)

            val returnedId = AdViewIdHelper.getFinalAdId(adOptions, adRequest, "test", contextMock)

            assertEquals(adOptions.adId, returnedId)
        }

        @Test
        @DisplayName("Returns the testingId when options are for testing and we are not in a testing device")
        fun testingWithoutTestingDevice() {
            val adOptions = AdOptions.TesterAdOptionsBuilder().setIsTesting(true).build()
            val adRequest = mock(AdRequest::class.java)
            Mockito.`when`(adRequest.isTestDevice(any())).thenReturn(false)

            val returnedId = AdViewIdHelper.getFinalAdId(adOptions, adRequest, "test", contextMock)

            assertEquals(adOptions.testingId, returnedId)
        }
    }
}
