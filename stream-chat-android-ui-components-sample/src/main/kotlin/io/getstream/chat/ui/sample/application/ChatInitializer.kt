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
import androidx.annotation.WorkerThread
import com.google.firebase.FirebaseApp
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.android.push.huawei.HuaweiPushDeviceGenerator
import io.getstream.android.push.xiaomi.XiaomiPushDeviceGenerator
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.notifications.handler.NotificationConfig
import io.getstream.chat.android.client.notifications.handler.NotificationHandlerFactory
import io.getstream.chat.android.client.uploader.FileUploader
import io.getstream.chat.android.client.utils.ProgressCallback
import io.getstream.chat.android.markdown.MarkdownTextTransformer
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.models.UploadedFile
import io.getstream.chat.android.models.UploadedImage
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.chat.android.ui.ChatUI
import io.getstream.chat.ui.sample.BuildConfig
import io.getstream.chat.ui.sample.debugger.CustomChatClientDebugger
import io.getstream.chat.ui.sample.feature.HostActivity
import java.io.File

typealias ChatResult<T> = io.getstream.result.Result<T>

class ChatInitializer(
    private val context: Context,
    private val autoTranslationEnabled: Boolean,
) {

    @Suppress("UNUSED_VARIABLE")
    fun init(apiKey: String) {
        FirebaseApp.initializeApp(context)
        val notificationConfig =
            NotificationConfig(
                pushDeviceGenerators = listOf(
                    FirebasePushDeviceGenerator(providerName = "Firebase"),
                    HuaweiPushDeviceGenerator(
                        context = context,
                        appId = ApplicationConfigurator.HUAWEI_APP_ID,
                        providerName = "huawei",
                    ),
                    XiaomiPushDeviceGenerator(
                        context = context,
                        appId = ApplicationConfigurator.XIAOMI_APP_ID,
                        appKey = ApplicationConfigurator.XIAOMI_APP_KEY,
                        providerName = "Xiaomi",
                    ),
                ),
                autoTranslationEnabled = autoTranslationEnabled,
            )
        val notificationHandler = NotificationHandlerFactory.createNotificationHandler(
            context = context,
            notificationConfig = notificationConfig,
            newMessageIntent = {
                    message: Message,
                    channel: Channel,
                ->
                HostActivity.createLaunchIntent(
                    context = context,
                    messageId = message.id,
                    parentMessageId = message.parentId,
                    channelType = channel.type,
                    channelId = channel.id,
                )
            },
        )
        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        val offlinePlugin = StreamOfflinePluginFactory(context)

        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = context,
        )

        val client = ChatClient.Builder(apiKey, context)
            .loggerHandler(FirebaseLogger)
            .notifications(notificationConfig, notificationHandler)
            .logLevel(logLevel)
            .withPlugins(offlinePlugin, statePluginFactory)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.CONNECTED)
            .fileUploader(ChatFileUploader())
            .apply {
                if (BuildConfig.DEBUG) {
                    this.debugRequests(true)
                        .clientDebugger(CustomChatClientDebugger())
                }
            }
            .build()

        // Using markdown as text transformer
        ChatUI.autoTranslationEnabled = autoTranslationEnabled
        ChatUI.messageTextTransformer = MarkdownTextTransformer(context) { item ->
            if (autoTranslationEnabled) {
                client.getCurrentUser()?.language?.let { language ->
                    item.message.getTranslation(language).ifEmpty { item.message.text }
                } ?: item.message.text
            } else {
                item.message.text
            }
        }

        // TransformStyle.messageComposerStyleTransformer = StyleTransformer { defaultStyle ->
        //     defaultStyle.copy(
        //         audioRecordingHoldToRecordText = "Bla bla bla",
        //         audioRecordingSlideToCancelText = "Wash to cancel",
        //     )
        // }
    }
}

private class ChatFileUploader : FileUploader {
    override fun sendFile(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
        callback: ProgressCallback,
    ): io.getstream.result.Result<UploadedFile> {
        TODO("Not yet implemented")
    }

    override fun sendFile(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
    ): io.getstream.result.Result<UploadedFile> {
        TODO("Not yet implemented")
    }

    override fun sendImage(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
        callback: ProgressCallback,
    ): ChatResult<UploadedImage> = uploadImage(
        channelId = channelId,
        file = file,
        callback = callback,
    )

    override fun sendImage(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
    ): io.getstream.result.Result<UploadedImage> {
        TODO("Not yet implemented")
    }

    override fun deleteFile(
        channelType: String,
        channelId: String,
        userId: String,
        url: String,
    ): io.getstream.result.Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun deleteImage(
        channelType: String,
        channelId: String,
        userId: String,
        url: String,
    ): io.getstream.result.Result<Unit> {
        TODO("Not yet implemented")
    }

    private fun uploadImage(
        channelId: String,
        file: File,
        callback: ProgressCallback? = null,
    ): ChatResult<UploadedImage> = uploadAttachment(channelId = channelId, file = file, callback = callback).let { uploadResult ->
        when (uploadResult.isSuccess) {
            true -> io.getstream.result.Result.Success(
                UploadedImage(file = uploadResult.getOrThrow()) // file is a signed gs: schemed URL
            )
            else -> io.getstream.result.Result.Failure(
                io.getstream.result.Error.ThrowableError(
                    uploadResult.exceptionOrNull()?.message!!,
                    uploadResult.exceptionOrNull()?.cause!!,
                )
            )
        }
    }

    @WorkerThread
    private fun uploadAttachment(
        channelId: String,
        file: File,
        callback: ProgressCallback?,
    ): Result<String> {
        Thread.sleep(5000)
        return Result.success("gs://$channelId/${file.name}")
    }
}