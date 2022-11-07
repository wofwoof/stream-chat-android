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

package io.getstream.chat.android.client.attachment.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import java.util.UUID

internal class UploadAttachmentsAndroidWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val channelType: String = inputData.getString(DATA_CHANNEL_TYPE)!!
        val channelId: String = inputData.getString(DATA_CHANNEL_ID)!!
        val messageId = inputData.getString(DATA_MESSAGE_ID)!!

        val chatClient = ChatClient.instance()
        val repositoryFacade = chatClient.repositoryFacade

        return UploadAttachmentsWorker(
            channelType = channelType,
            channelId = channelId,
            channelStateLogic = chatClient.logicRegistry?.channelStateLogic(channelType, channelId),
            messageRepository = repositoryFacade,
            chatClient = chatClient
        ).uploadAttachmentsForMessage(
            messageId
        ).let { result ->
            when (result) {
                is io.getstream.chat.android.client.utils.Result.Success -> Result.success()
                is io.getstream.chat.android.client.utils.Result.Failure -> Result.failure()
            }
        }
    }

    companion object {
        private const val DATA_MESSAGE_ID = "message_id"
        private const val DATA_CHANNEL_TYPE = "channel_type"
        private const val DATA_CHANNEL_ID = "channel_id"

        fun start(
            context: Context,
            channelType: String,
            channelId: String,
            messageId: String,
            networkType: UploadAttachmentsNetworkType,
        ): UUID {
            val uploadAttachmentsWorkRequest = OneTimeWorkRequestBuilder<UploadAttachmentsAndroidWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType.toNetworkType()).build())
                .setInputData(
                    workDataOf(
                        DATA_CHANNEL_ID to channelId,
                        DATA_CHANNEL_TYPE to channelType,
                        DATA_MESSAGE_ID to messageId,
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$channelId$messageId",
                ExistingWorkPolicy.KEEP,
                uploadAttachmentsWorkRequest
            )
            return uploadAttachmentsWorkRequest.id
        }

        /**
         * Stops the ongoing work if there is any.
         *
         * @param context Context of the application.
         * @param workId UUID of the enqueued work.
         */
        fun stop(context: Context, workId: UUID) {
            WorkManager.getInstance(context).cancelWorkById(workId)
        }
    }
}
