/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-chat-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.chat.ui.sample.application

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.firebase.FirebaseApp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.notifications.handler.NotificationConfig
import io.getstream.chat.android.client.notifications.handler.NotificationHandlerFactory
import io.getstream.chat.android.client.utils.ImageLoader
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.markdown.MarkdownTextTransformer
import io.getstream.chat.android.offline.plugin.configuration.Config
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.pushprovider.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.pushprovider.huawei.HuaweiPushDeviceGenerator
import io.getstream.chat.android.pushprovider.xiaomi.XiaomiPushDeviceGenerator
import io.getstream.chat.android.ui.ChatUI
import io.getstream.chat.ui.sample.BuildConfig
import io.getstream.chat.ui.sample.feature.HostActivity
import io.getstream.logging.StreamLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatInitializer(private val context: Context) {

    @Suppress("UNUSED_VARIABLE")
    fun init(apiKey: String) {
        FirebaseApp.initializeApp(context)

        val imageLoader = buildImageLoader()

        val notificationHandler = NotificationHandlerFactory.createNotificationHandler(
            context = context,
            newMessageIntent = {
                    messageId: String,
                    channelType: String,
                    channelId: String,
                ->
                HostActivity.createLaunchIntent(context, messageId, channelType, channelId)
            },
            imageLoader = imageLoader
        )
        val notificationConfig =
            NotificationConfig(
                pushDeviceGenerators = listOf(
                    FirebasePushDeviceGenerator(),
                    HuaweiPushDeviceGenerator(context, ApplicationConfigurator.HUAWEI_APP_ID),
                    XiaomiPushDeviceGenerator(
                        context,
                        ApplicationConfigurator.XIAOMI_APP_ID,
                        ApplicationConfigurator.XIAOMI_APP_KEY,
                    ),
                ),
            )
        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        val offlinePlugin = StreamOfflinePluginFactory(Config(userPresence = true, persistenceEnabled = true), context)

        val client = ChatClient.Builder(apiKey, context)
            .loggerHandler(FirebaseLogger)
            .notifications(notificationConfig, notificationHandler)
            .logLevel(logLevel)
            .withPlugin(offlinePlugin)
            .debugRequests(true)
            .build()

        // Using markdown as text transformer
        ChatUI.messageTextTransformer = MarkdownTextTransformer(context)
    }

    private fun buildImageLoader() = ImageLoader { uri ->
        suspendCancellableCoroutine { cont ->
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
