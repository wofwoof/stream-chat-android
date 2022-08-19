package com.getstream.sdk.chat.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.utils.ImageLoader
import io.getstream.chat.android.client.utils.Result
import io.getstream.logging.StreamLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

public class CoilImageLoader(
    private val context: Context
) : ImageLoader {
    override suspend fun loadImage(uri: String): Result<Bitmap> {
        return suspendCancellableCoroutine { cont ->
            var disposable: Disposable? = null
            cont.invokeOnCancellation {
                disposable?.dispose()
            }
            val req = ImageRequest.Builder(context)
                .data(uri)
                .transformations(CircleCropTransformation())
                .target(onSuccess = { result ->
                    val bitmap = (result as BitmapDrawable).bitmap
                    cont.resume(Result.success(bitmap))
                }, onError = {
                    cont.resume(Result.error(ChatError(
                        message = "Failed to load image by uri: $uri"
                    )))
                })
                .build()
            StreamLog.d("CoilImageLoader") { "[loadImage] uri: $uri" }
            disposable = Coil.imageLoader(context).enqueue(req)

        }
    }
}