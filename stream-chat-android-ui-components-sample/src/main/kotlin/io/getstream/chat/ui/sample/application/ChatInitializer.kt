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
import com.google.firebase.FirebaseApp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.client.models.UploadedFile
import io.getstream.chat.android.client.models.UploadedImage
import io.getstream.chat.android.client.notifications.handler.NotificationConfig
import io.getstream.chat.android.client.notifications.handler.NotificationHandlerFactory
import io.getstream.chat.android.client.uploader.FileUploader
import io.getstream.chat.android.client.utils.ProgressCallback
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.markdown.MarkdownTextTransformer
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.pushprovider.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.pushprovider.huawei.HuaweiPushDeviceGenerator
import io.getstream.chat.android.pushprovider.xiaomi.XiaomiPushDeviceGenerator
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.chat.android.ui.ChatUI
import io.getstream.chat.ui.sample.BuildConfig
import io.getstream.chat.ui.sample.feature.HostActivity
import java.io.File

class ChatInitializer(private val context: Context) {

    @Suppress("UNUSED_VARIABLE")
    fun init(apiKey: String) {
        FirebaseApp.initializeApp(context)
        val notificationHandler = NotificationHandlerFactory.createNotificationHandler(
            context = context,
            newMessageIntent = {
                    messageId: String,
                    channelType: String,
                    channelId: String,
                ->
                HostActivity.createLaunchIntent(context, messageId, channelType, channelId)
            }
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
                requestPermissionOnAppLaunch = { true }
            )
        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        val offlinePlugin = StreamOfflinePluginFactory(context)

        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = context
        )

        val client = ChatClient.Builder(apiKey, context)
            .loggerHandler(FirebaseLogger)
            .notifications(notificationConfig, notificationHandler)
            .logLevel(logLevel)
            .withPlugins(offlinePlugin, statePluginFactory)
            .debugRequests(true)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .fileUploader(DummyFileUploader())
            .build()

        // Using markdown as text transformer
        ChatUI.messageTextTransformer = MarkdownTextTransformer(context)
    }
}

private class DummyFileUploader : FileUploader {

    override fun sendImage(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
        callback: ProgressCallback,
    ): Result<UploadedImage> {
        return Result.Failure(ChatError.GenericError("ABC"))
    }

    override fun sendImage(
        channelType: String,
        channelId: String,
        userId: String,
        file: File
    ): Result<UploadedImage> {
        return Result.Failure(ChatError.GenericError("ABC"))
    }

    override fun sendFile(
        channelType: String,
        channelId: String,
        userId: String,
        file: File,
        callback: ProgressCallback,
    ): Result<UploadedFile> {
        TODO("Not yet implemented")
    }

    override fun sendFile(channelType: String, channelId: String, userId: String, file: File): Result<UploadedFile> {
        TODO("Not yet implemented")
    }

    override fun deleteFile(channelType: String, channelId: String, userId: String, url: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun deleteImage(channelType: String, channelId: String, userId: String, url: String): Result<Unit> {
        TODO("Not yet implemented")
    }
}

