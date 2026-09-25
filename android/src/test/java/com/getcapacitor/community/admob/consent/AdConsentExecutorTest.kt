package com.getcapacitor.community.admob.consent

import android.app.Activity
import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.common.util.BiConsumer
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AdConsentExecutorTest {
    @Mock
    private lateinit var contextMock: Context

    @Mock(strictness = Mock.Strictness.LENIENT)
    private lateinit var activityMock: Activity

    @Mock
    private lateinit var notifierMock: BiConsumer<String, JSObject>

    @Mock
    private lateinit var mockedConsentInformation: ConsentInformation

    private lateinit var mockedUserMessagingPlatform: MockedStatic<UserMessagingPlatform>

    private lateinit var listenerCaptor: ArgumentCaptor<ConsentForm.OnConsentFormDismissedListener>

    @Mock
    private lateinit var pluginCallMock: PluginCall

    private lateinit var adConsentExecutor: AdConsentExecutor

    @BeforeEach
    fun beforeEach() {
        mockedUserMessagingPlatform = Mockito.mockStatic(UserMessagingPlatform::class.java)
        mockedUserMessagingPlatform
            .`when`<ConsentInformation> { UserMessagingPlatform.getConsentInformation(any(Context::class.java)) }
            .thenReturn(mockedConsentInformation)
        listenerCaptor = ArgumentCaptor.forClass(ConsentForm.OnConsentFormDismissedListener::class.java)

        adConsentExecutor = AdConsentExecutor({ contextMock }, { activityMock }, notifierMock, LOG_TAG)
    }

    @AfterEach
    fun afterEach() {
        mockedUserMessagingPlatform.close()
    }

    @Nested
    @DisplayName("Show Privacy Options Form Tests")
    inner class ShowPrivacyOptionsFormTests {
        @Test
        @DisplayName("should resolve the call on success")
        fun showPrivacyOptionsFormSuccess() {
            adConsentExecutor.showPrivacyOptionsForm(pluginCallMock, null)
            mockedUserMessagingPlatform.verify {
                UserMessagingPlatform.showPrivacyOptionsForm(eq(activityMock), listenerCaptor.capture())
            }
            listenerCaptor.value.onConsentFormDismissed(null)
            verify(pluginCallMock).resolve()
        }

        @Test
        @DisplayName("should reject the call on failure")
        fun showPrivacyOptionsFormFailure() {
            val testError = FormError(123, "Test privacy form error")
            adConsentExecutor.showPrivacyOptionsForm(pluginCallMock, null)
            mockedUserMessagingPlatform.verify {
                UserMessagingPlatform.showPrivacyOptionsForm(eq(activityMock), listenerCaptor.capture())
            }
            listenerCaptor.value.onConsentFormDismissed(testError)
            verify(pluginCallMock).reject("Error when show privacy form", testError.message)
        }

        @Test
        @DisplayName("should reject the call if activity is null")
        fun showPrivacyOptionsFormNullActivity() {
            adConsentExecutor = AdConsentExecutor({ contextMock }, { null }, notifierMock, LOG_TAG)
            adConsentExecutor.showPrivacyOptionsForm(pluginCallMock, null)
            verify(pluginCallMock).reject("Trying to show the privacy options form but the Activity is null")
        }
    }

    private companion object {
        private const val LOG_TAG = "AdConsentExecutorTest Log Tag"
    }
}
