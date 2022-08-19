package io.getstream.chat.android.client.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmapOrNull
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.logging.StreamLog
import java.net.URL

public fun interface ImageLoader {

    public suspend fun loadImage(uri: String): Result<Bitmap>

}

internal class DefaultImageLoader(private val context: Context) : ImageLoader {
    override suspend fun loadImage(uri: String): Result<Bitmap> {
        StreamLog.i("DefaultImageLoader") { "[loadImage] uri: $uri" }
        return try {
            val bitmap = URL(uri).openStream().use {
                RoundedBitmapDrawableFactory.create(
                    context.resources,
                    BitmapFactory.decodeStream(it),
                )
                    .apply { isCircular = true }
                    .toBitmapOrNull()
            }
            when (bitmap == null) {
                true -> Result.error(ChatError())
                else -> Result.success(bitmap)
            }
        } catch (e: Throwable) {
            Result.error(e)
        }
    }
}