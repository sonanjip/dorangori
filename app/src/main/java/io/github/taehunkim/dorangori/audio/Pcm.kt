package io.github.taehunkim.dorangori.audio

object Pcm {
    const val RATE = 16_000
}

/** One recognised utterance. */
data class Cue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Streaming conversion of arbitrary interleaved PCM to mono at 16 kHz. Channels are averaged; sample-rate
 * conversion uses area averaging, which is enough for speech and avoids aliasing when downsampling.
 */
class StreamingResampler(private val srcRate: Int, private val channels: Int, private val dstRate: Int = Pcm.RATE) {
    private val step = srcRate.toDouble() / dstRate
    private var acc = 0.0
    private var covered = 0.0

    /** [interleaved] holds `channels` values per frame, scaled to -1..1. Returns mono samples at the target rate. */
    fun process(interleaved: FloatArray, frames: Int, out: MutableList<Short>) {
        for (f in 0 until frames) {
            var s = 0f
            for (c in 0 until channels) s += interleaved[f * channels + c]
            s /= channels
            var remaining = 1.0
            while (remaining > 1e-9) {
                val room = step - covered
                val take = minOf(room, remaining)
                acc += s * take; covered += take; remaining -= take
                if (covered >= step - 1e-9) {
                    out += (acc / step * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                    acc = 0.0; covered = 0.0
                }
            }
        }
    }
}
