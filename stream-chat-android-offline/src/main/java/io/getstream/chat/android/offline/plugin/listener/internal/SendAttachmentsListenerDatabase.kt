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

package io.getstream.chat.android.offline.plugin.listener.internal

import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.errors.ChatErrorCode
import io.getstream.chat.android.client.errors.isPermanent
import io.getstream.chat.android.client.extensions.internal.users
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.persistance.repository.ChannelRepository
import io.getstream.chat.android.client.persistance.repository.MessageRepository
import io.getstream.chat.android.client.persistance.repository.UserRepository
import io.getstream.chat.android.client.plugin.listeners.SendAttachmentListener
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.client.utils.internal.toMessageSyncDescription
import io.getstream.logging.StreamLog
import java.util.Date

private const val TAG = "Chat:SendAttachmentsListenerDB"

/**
 * Updates the database of the SDK accordingly with the request to send the attachments to the backend.
 */
internal class SendAttachmentsListenerDatabase(
    private val messageRepository: MessageRepository,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository
) : SendAttachmentListener {

    /**
     * Update the database of the SDK before the attachments are sent to the backend.
     *
     * @param channelType String
     * @param channelId String
     * @param message [Message]
     */
    override suspend fun onAttachmentSendRequest(channelType: String, channelId: String, message: Message) {
        // we insert early to ensure we don't lose messages
        messageRepository.insertMessage(message)
        channelRepository.updateLastMessageForChannel(message.cid, message)
    }

    override suspend fun onAttachmentSendResult(
        channelType: String,
        channelId: String,
        message: Message,
        result: Result<Message>,
    ) {
        if (result.isFailure) {
            handleSendMessageFail(message, result.chatErrorOrNull()!!)
        }
    }

    private suspend fun handleSendMessageFail(
        message: Message,
        error: ChatError,
    ) {
        val isPermanentError = error.isPermanent()
        val isMessageModerationFailed = error is ChatError.NetworkError &&
            error.streamCode == ChatErrorCode.MESSAGE_MODERATION_FAILED.code

        StreamLog.w(TAG) {
            "[handleSendMessageFail] isPermanentError: $isPermanentError" +
                ", isMessageModerationFailed: $isMessageModerationFailed"
        }

        message.copy(
            syncStatus = if (isPermanentError) {
                SyncStatus.FAILED_PERMANENTLY
            } else {
                SyncStatus.SYNC_NEEDED
            },
            syncDescription = error.toMessageSyncDescription(),
            updatedLocallyAt = Date(),
        ).also { parsedMessage ->
            userRepository.insertUsers(parsedMessage.users())
            messageRepository.insertMessage(parsedMessage, cache = false)
        }
    }
}
