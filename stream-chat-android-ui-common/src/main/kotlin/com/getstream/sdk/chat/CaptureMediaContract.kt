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

package com.getstream.sdk.chat

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.chat.android.ui.common.R
import io.getstream.logging.StreamLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

@InternalStreamChatApi
public class CaptureMediaContract(
    public var pictureFile: File? = null,
    public var videoFile: File? = null,
) : ActivityResultContract<Unit, File?>() {

    private val logger = StreamLog.getLogger("CameraAttachContract")

    override fun createIntent(context: Context, input: Unit): Intent {
        val takePictureIntents =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir,
                createFileName("STREAM_IMG", "jpg")
            ).let {
                logger.v { "[createIntent] pictureFile: $it" }
                pictureFile = it
                createIntentList(context, MediaStore.ACTION_IMAGE_CAPTURE, it)
            }
        val recordVideoIntents =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.cacheDir,
                createFileName("STREAM_VID", "mp4")
            ).let {
                logger.v { "[createIntent] videoFile: $it" }
                videoFile = it
                createIntentList(context, MediaStore.ACTION_VIDEO_CAPTURE, it)
            }
        val intents = takePictureIntents + recordVideoIntents
        val initialIntent = intents.lastOrNull() ?: Intent()
        return Intent.createChooser(initialIntent, context.getString(R.string.stream_ui_message_input_capture_media))
            .apply {
                putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    (intents - initialIntent).toTypedArray()
                )
            }
            .also {
                logger.v { "[createIntent] intent: ${it.stringify()}" }
            }
    }

    private fun createFileName(prefix: String, extension: String) =
        "${prefix}_${dateFormat.format(Date().time)}.$extension"

    private fun createIntentList(
        context: Context,
        action: String,
        destinationFile: File,
    ): List<Intent> {
        val destinationUri = StreamFileUtil.getUriForFile(
            context,
            destinationFile
        )
        val actionIntent = Intent(action)
        return context.packageManager.queryIntentActivities(
            actionIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).map {
            Intent(actionIntent).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
                flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                `package` = it.activityInfo.packageName
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): File? {
        logger.i { "[parseResult] resultCode: $resultCode, intent: ${intent?.stringify()}" }
        logger.i { "[parseResult] pictureFile(${pictureFile?.exists()} - ${pictureFile?.length()}): $pictureFile" }
        logger.i { "[parseResult] pictureFile(${videoFile?.exists()} - ${videoFile?.length()}): $videoFile" }
        return (pictureFile.takeIfCaptured() ?: videoFile.takeIfCaptured())
            .takeIf { resultCode == Activity.RESULT_OK }
    }

    override fun toString(): String {
        return "CaptureMediaContract(pictureFile=$pictureFile, videoFile=$videoFile)"
    }
}

private fun File?.takeIfCaptured(): File? = this?.takeIf { it.exists() && it.length() > 0 }


public fun Intent.stringify(): String {
    return "Intent(action=$action, extras=$extras)"
}

public fun Intent.stringify2(): String {
    return """
Intent(
  action=$action, 
  package=${`package`}, 
  categories=$categories, 
  data=$data, 
  extras=${extras?.stringify()}
)
    """.trimIndent()
}

public fun Bundle?.stringify(): String {
    this ?: return "null"
    val out = StringBuilder("Bundle{\n")
    for (key in keySet()) {
        out.append(key).append(": ")
        when (val value = get(key)) {
            is Bundle -> out.append(value.stringify()).append(",\n")
            is Intent -> out.append(value.stringify()).append(",\n")
            is Array<*> -> out.append(value.stringify()).append(",\n")
            else -> out.append(value).append(",\n")
        }
    }
    out.append("}\n")
    return out.toString()
}

private fun Array<*>.stringify(): String {
    val out = StringBuilder("Array[\n")
    forEach { subValue ->
        when (subValue) {
            is Bundle -> out.append(subValue.stringify()).append(",\n")
            is Intent -> out.append(subValue.stringify()).append(",\n")
            is Array<*> -> out.append(subValue.stringify()).append(",\n")
            else -> out.append(subValue).append(",\n")
        }
    }
    out.append("]\n");
    return out.toString()
}

public fun main() {
    println(arrayListOf("12", "34"))
}