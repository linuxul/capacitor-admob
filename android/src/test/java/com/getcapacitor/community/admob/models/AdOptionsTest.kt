package com.getcapacitor.community.admob.models

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.eqK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.lenient
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AdOptionsTest {
    @Mock
    lateinit var pluginCallMock: PluginCall

    @BeforeEach
    fun beforeEach() {
        reset(pluginCallMock)
        // Dummy Returns
        `when`(pluginCallMock.getString(anyString(), anyString())).thenReturn("DEFAULT_RETURN")
    }

    // Just for now lets check the call and default value in the same tests.
    @Nested
    @DisplayName("Gets Property from plugin call or default")
    inner class GetProperty {
        @Test
        fun adId() {
            val expected = "Some Given Test Id"
            `when`(pluginCallMock.getString(eqK("adId"), anyString())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            assertEquals(expected, adOptions.adId)
        }

        @Test
        fun position() {
            val wantedProperty = "position"
            val expected = "Some Position"
            val defaultValue = "BOTTOM_CENTER"
            `when`(pluginCallMock.getString(eqK(wantedProperty), anyString())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            verify(pluginCallMock).getString(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.position)
        }

        @Test
        fun margin() {
            val wantedProperty = "margin"
            val expected = 69
            val defaultValue = 0
            `when`(pluginCallMock.getInt(eqK(wantedProperty), anyInt())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            verify(pluginCallMock).getInt(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.margin)
        }

        @Test
        fun isTesting() {
            val wantedProperty = "isTesting"
            val expected = true
            val defaultValue = false
            `when`(pluginCallMock.getBoolean(eqK(wantedProperty), anyBoolean())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            verify(pluginCallMock).getBoolean(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.isTesting)
        }

        @Test
        fun npa() {
            val wantedProperty = "npa"
            val expected = true
            val defaultValue = false
            lenient().`when`(pluginCallMock.getBoolean(eqK(wantedProperty), anyBoolean())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            verify(pluginCallMock).getBoolean(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.npa)
        }

        @Test
        fun ssv() {
            val customData = "customData"
            val userId = "userId"
            val wantedProperty = "ssv"
            val expected = JSObject()
            expected.put(customData, customData)
            expected.put(userId, userId)
            lenient().`when`(pluginCallMock.getObject(eqK(wantedProperty), isNull())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createGenericOptions(pluginCallMock, "")

            verify(pluginCallMock, atLeastOnce()).getObject(wantedProperty)
            assertEquals(userId, adOptions.ssvInfo.userId)
            assertEquals(customData, adOptions.ssvInfo.customData)
        }

        @Test
        fun appOpenAdId() {
            val expected = "Some Given AppOpen Test Id"
            `when`(pluginCallMock.getString(eqK("adId"), anyString())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            assertEquals(expected, adOptions.adId)
        }

        @Test
        fun appOpenPosition() {
            val wantedProperty = "position"
            val expected = "TOP_CENTER"
            val defaultValue = "BOTTOM_CENTER"
            `when`(pluginCallMock.getString(eqK(wantedProperty), anyString())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            verify(pluginCallMock).getString(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.position)
        }

        @Test
        fun appOpenMargin() {
            val wantedProperty = "margin"
            val expected = 10
            val defaultValue = 0
            `when`(pluginCallMock.getInt(eqK(wantedProperty), anyInt())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            verify(pluginCallMock).getInt(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.margin)
        }

        @Test
        fun appOpenIsTesting() {
            val wantedProperty = "isTesting"
            val expected = true
            val defaultValue = false
            `when`(pluginCallMock.getBoolean(eqK(wantedProperty), anyBoolean())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            verify(pluginCallMock).getBoolean(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.isTesting)
        }

        @Test
        fun appOpenNpa() {
            val wantedProperty = "npa"
            val expected = true
            val defaultValue = false
            lenient().`when`(pluginCallMock.getBoolean(eqK(wantedProperty), anyBoolean())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            verify(pluginCallMock).getBoolean(wantedProperty, defaultValue)
            assertEquals(expected, adOptions.npa)
        }

        @Test
        fun appOpenSsv() {
            val customData = "customData"
            val userId = "userId"
            val wantedProperty = "ssv"
            val expected = JSObject()
            expected.put(customData, customData)
            expected.put(userId, userId)
            lenient().`when`(pluginCallMock.getObject(eqK(wantedProperty), isNull())).thenReturn(expected)

            val adOptions = AdOptions.AdOptionsFactory.createAppOpenOptions(pluginCallMock)

            verify(pluginCallMock, atLeastOnce()).getObject(wantedProperty)
            assertEquals(userId, adOptions.ssvInfo.userId)
            assertEquals(customData, adOptions.ssvInfo.customData)
        }
    }
}
