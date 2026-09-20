package com.getcapacitor.community.admob.helpers

import android.os.Bundle
import com.getcapacitor.community.admob.models.AdOptions
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class RequestHelperTest {
    @Test
    @DisplayName("#createRequest should add npa to the bundle if npa is requested")
    fun npa() {
        // createRequest returns a non-null AdRequest, so the mocked builder has to build one.
        Mockito.mockConstruction(AdRequest.Builder::class.java) { builder, _ ->
            Mockito.`when`(builder.build()).thenReturn(mock(AdRequest::class.java))
        }.use { adRequestBuilderMockedConstruction ->
            Mockito.mockConstruction(Bundle::class.java).use { bundleMockedConstruction ->
                val adOptions = AdOptions.TesterAdOptionsBuilder().setNpa(true).build()

                // Act
                RequestHelper.createRequest(adOptions)

                val mockedBundle = bundleMockedConstruction.constructed()[0]
                val adRequestBuilder = adRequestBuilderMockedConstruction.constructed()[0]
                verify(mockedBundle).putString("npa", "1")
                verify(adRequestBuilder).addNetworkExtrasBundle(AdMobAdapter::class.java, mockedBundle)
            }
        }
    }
}
