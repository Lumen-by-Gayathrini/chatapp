package com.gayathrini.chatapp.ui.stickers

import androidx.annotation.DrawableRes
import com.gayathrini.chatapp.R

/** A bundled sticker (TDD §6.20): a stable [id] sent to the server + the local [res] to render. */
data class Sticker(val id: String, @DrawableRes val res: Int)

/**
 * The client-side bundled sticker pack (TDD §6.20). The server only stores the sticker [id];
 * rendering resolves it back to a bundled drawable here. Unknown ids resolve to null.
 */
object StickerPack {
    val all: List<Sticker> = listOf(
        Sticker("smile", R.drawable.ic_sticker_smile),
        Sticker("laugh", R.drawable.ic_sticker_laugh),
        Sticker("sad", R.drawable.ic_sticker_sad),
        Sticker("heart", R.drawable.ic_sticker_heart),
        Sticker("star", R.drawable.ic_sticker_star),
        Sticker("thumbsup", R.drawable.ic_sticker_thumbsup),
    )

    private val byId: Map<String, Sticker> = all.associateBy { it.id }

    /** The drawable for a sticker id, or null when the id isn't in the bundled pack. */
    @DrawableRes
    fun drawableFor(id: String?): Int? = id?.let { byId[it]?.res }
}
