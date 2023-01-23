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

package io.getstream.chat.android.compose.sample

import android.content.Context
import android.util.Log
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.notifications.handler.NotificationConfig
import io.getstream.chat.android.client.notifications.handler.NotificationHandlerFactory
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.compose.sample.data.UserCredentials
import io.getstream.chat.android.compose.sample.ui.StartupActivity
import io.getstream.chat.android.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.extensions.globalState
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

/**
 * A helper class that is responsible for initializing the SDK and connecting/disconnecting
 * a user. Under the hood, it persists the user so that we are able to connect automatically
 * next time the app is launched.
 */
object ChatHelper {

    private const val TAG = "ChatHelper"

    /**
     * Initializes the SDK with the given API key.
     */
    fun initializeSdk(context: Context, apiKey: String) {
        Log.d(TAG, "[init] apiKey: $apiKey")
        val notificationConfig = NotificationConfig(
            pushDeviceGenerators = listOf(FirebasePushDeviceGenerator())
        )
        val notificationHandler = NotificationHandlerFactory.createNotificationHandler(
            context = context,
            newMessageIntent = { messageId: String, channelType: String, channelId: String ->
                StartupActivity.createIntent(
                    context = context,
                    channelId = "$channelType:$channelId",
                    messageId = messageId
                )
            }
        )

        val offlinePlugin = StreamOfflinePluginFactory(context)

        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = context
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        ChatClient.Builder(apiKey, context)
            .notifications(notificationConfig, notificationHandler)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .build()
    }

    /**
     * Initializes [ChatClient] with the given user and saves it to the persistent storage.
     */
    fun connectUser(
        userCredentials: UserCredentials,
        onSuccess: () -> Unit = {},
        onError: (ChatError) -> Unit = {},
    ) {
        ChatClient.instance().run {
            if (globalState.user.value == null) {
                connectUser(userCredentials.user, userCredentials.token)
                    .enqueue { result ->
                        when (result) {
                            is Result.Success -> {
                                ChatApp.credentialsRepository.saveUserCredentials(userCredentials)
                                onSuccess()
                            }
                            is Result.Failure -> onError(result.value)
                        }
                    }
            } else {
                onSuccess()
            }
        }
    }

    /**
     * Logs out the user and removes their credentials from the persistent storage.
     */
    suspend fun disconnectUser() {
        ChatApp.credentialsRepository.clearCredentials()

        ChatClient.instance().disconnect(false).await()
    }
}
