/*
 * Created by Everysight LTD.
 *
 * The glasses speaker, and the two ways sound reaches it.
 *
 *   playSound(M2AudioResource)   a whole file, uploaded once and triggered afterwards
 *   openStream(M2AudioStreamInput.Url) an endless source the SDK pulls and transcodes itself
 *
 * They are not variants of one call. A resource is finite, so it can be cached on the glasses
 * and replayed for free; a stream never ends, so it can only be piped. Everything the glasses
 * play is LC3 — the SDK transcodes on the phone, which is why an MP3 radio URL works without
 * the app owning a decoder.
 *
 * This screen is only the read-out. The calls live in Mav2ComposeController.runSpeakerAction.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

/**
 * A title, what is playing, and whatever the stream last said about itself.
 *
 * The third line exists because an ICY radio stream announces its current track, and that
 * announcement is the only proof from the glasses side that real audio is flowing rather
 * than a socket merely being open.
 */
class SpeakerDemoScreen : M2Screen(width = 420f, height = 300f, tag = "sample-speaker") {

    private var source: M2Text? = null
    private var detail: M2Text? = null

    override fun onCreate() {
        super.onCreate()

        val title = M2Text("Speaker", "speaker-title").apply {
            setColor(M2Color.White)
            setXY(width / 2f, 90f)
            setAlign(M2Align.CenterHorizontal)
        }
        val source = M2Text("Silent", "speaker-source").apply {
            setColor(M2Color.Yellow)
            setXY(width / 2f, 140f)
            setAlign(M2Align.CenterHorizontal)
        }
        val detail = M2Text("", "speaker-detail").apply {
            setColor(M2Color.White.withAlpha(180u))
            setXY(width / 2f, 180f)
            setAlign(M2Align.CenterHorizontal)
        }

        this.source = source
        this.detail = detail

        add(title)
        add(source)
        add(detail)
    }

    override fun onRelease() {
        source = null
        detail = null
        super.onRelease()
    }

    /** Names what the speaker is playing, and clears any previous detail line. */
    fun show(sourceText: String, detailText: String = "") {
        source?.setText(sourceText)
        detail?.setText(detailText)
    }

    /** Replaces only the second line — used for the stream's own track announcements. */
    fun showDetail(detailText: String) {
        detail?.setText(detailText)
    }
}
