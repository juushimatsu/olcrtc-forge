package io.github.oleglog.olcrtc.client.profile.olcrtc

import io.github.oleglog.olcrtc.client.importer.DecodedImportPayload
import io.github.oleglog.olcrtc.client.importer.ImportPayload
import io.github.oleglog.olcrtc.client.importer.SubscriptionPayload
import io.github.oleglog.olcrtc.client.profile.ImportedProfile
import io.github.oleglog.olcrtc.client.profile.ProfileUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OlcboxImportTest {
    private val key = "a".repeat(64)

    @Test
    fun importsPanelQrAsStandaloneProfileAndRoundTrips() {
        val raw = "olcrtc://wbstream?vp8channel<vp8-fps=60&vp8-batch=32>@room-one#$key\$Москва + основной"
        val decoded = ImportPayload.decode(raw) as DecodedImportPayload.Profile
        val profile = (ProfileUri.parse(decoded.uri) as ImportedProfile.Olcrtc).value
        assertEquals("Москва + основной", profile.name)
        assertEquals("room-one", profile.roomId)
        assertEquals(60, profile.vp8Fps)
        assertEquals(32, profile.vp8BatchSize)
        assertEquals(OlcrtcProfile.CompatibilityMode.CURRENT, profile.compatibilityMode)
        assertEquals(profile, OlcrtcUri.parse(OlcrtcUri.serialize(profile)))
        assertEquals(profile.clientId, OlcrtcUri.parse(raw.substringBefore('$') + "\$Renamed").clientId)
    }

    @Test
    fun acceptsOlcboxDefaultsAndRoomUrls() {
        val profile = OlcrtcUri.parse("olcrtc://telemost?vp8channel@123456789#$key")
        assertEquals(30, profile.vp8Fps)
        assertEquals(64, profile.vp8BatchSize)
        val jitsi = OlcrtcUri.parse("olcrtc://jitsi?datachannel@https://meet.example/room+one#$key\$Office")
        assertEquals("https://meet.example/room+one", jitsi.roomId)
    }

    @Test
    fun acceptsBothFormatsInSubscriptionsAndIndividualImports() {
        val links = listOf(
            "olcrtc://telemost?vp8channel@123456789#$key\$Panel",
            "olcrtc://jitsi@r/https%3A%2F%2Fmeet.example%2Froom?k=$key&t=datachannel&c=client#Direct",
        )
        val subscription = SubscriptionPayload.parse(links.joinToString("\n").toByteArray())
        assertEquals(links.map(ProfileUri::parse), subscription.profiles)
        assertEquals(emptyList<String>(), subscription.rejectedProfiles)
        links.forEach { link ->
            assertEquals(DecodedImportPayload.Profile(link), ImportPayload.decode(link))
        }
    }

    @Test
    fun rejectsMalformedOrUnsupportedOlcboxWithoutIgnoringSettings() {
        listOf(
            "olcrtc://wbstream?vp8channel<vp8-fps=30&vp8-fps=60>@room#$key",
            "olcrtc://wbstream?vp8channel<vp8-fps=no>@room#$key",
            "olcrtc://wbstream?vp8channel<vp8-batch=0>@room#$key",
            "olcrtc://wbstream?vp8channel<unknown=1>@room#$key",
            "olcrtc://jitsi?datachannel<vp8-fps=30>@room#$key",
            "olcrtc://wbstream?seichannel@room#$key",
            "olcrtc://wbstream?vp8channel@room#bad-key",
            "olcrtc://wbstream?vp8channel@#$key",
            "olcrtc://wbstream?vp8channel@room#$key\$line\nbreak",
        ).forEach { raw ->
            assertThrows(raw, IllegalArgumentException::class.java) { OlcrtcUri.parse(raw) }
        }
    }
}
