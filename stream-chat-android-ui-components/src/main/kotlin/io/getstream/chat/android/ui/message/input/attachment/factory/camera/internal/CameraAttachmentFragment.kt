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

package io.getstream.chat.android.ui.message.input.attachment.factory.camera.internal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.getstream.sdk.chat.CaptureMediaContract
import com.getstream.sdk.chat.model.AttachmentMetaData
import com.getstream.sdk.chat.stringify
import com.getstream.sdk.chat.utils.PermissionChecker
import io.getstream.chat.android.ui.common.extensions.internal.streamThemeInflater
import io.getstream.chat.android.ui.common.style.setTextStyle
import io.getstream.chat.android.ui.databinding.StreamUiFragmentAttachmentCameraBinding
import io.getstream.chat.android.ui.message.input.MessageInputViewStyle
import io.getstream.chat.android.ui.message.input.attachment.AttachmentSource
import io.getstream.chat.android.ui.message.input.attachment.factory.AttachmentsPickerTabListener
import io.getstream.logging.StreamLog
import java.io.File

/**
 * Represents the tab of the attachment picker with media capture.
 */
internal class CameraAttachmentFragment : Fragment() {

    private val logger = StreamLog.getLogger("CameraAttachView")

    private var _binding: StreamUiFragmentAttachmentCameraBinding? = null
    private val binding get() = _binding!!

    private val permissionChecker: PermissionChecker = PermissionChecker()
    private var captureMediaContract: CaptureMediaContract? = null
    private var activityResultLauncher: ActivityResultLauncher<Unit>? = null

    /**
     * Style for the attachment picker dialog.
     */
    private lateinit var style: MessageInputViewStyle

    /**
     * A listener invoked when attachments are selected in the attachment tab.
     */
    private var attachmentsPickerTabListener: AttachmentsPickerTabListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        logger.d { "[onCreateView] savedInstanceState: $savedInstanceState" }
        _binding =
            StreamUiFragmentAttachmentCameraBinding.inflate(requireContext().streamThemeInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logger.d { "[onViewCreated] savedInstanceState: $savedInstanceState" }
        super.onViewCreated(view, savedInstanceState)
        if (::style.isInitialized) {
            setupViews()
            setupResultListener()
            checkPermissions()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val pictureFile = savedInstanceState?.getString(KEY_PICTURE)?.let { File(it) }
        val videoFile = savedInstanceState?.getString(KEY_VIDEO)?.let { File(it) }
        logger.d { "[onViewStateRestored] pictureFile: $pictureFile, videoFile: $videoFile" }
        if (pictureFile != null || videoFile != null) {
            captureMediaContract = CaptureMediaContract(
                pictureFile = pictureFile,
                videoFile = videoFile,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureMediaContract.also {
            logger.d { "[onSaveInstanceState] pictureFile: ${it?.pictureFile}, videoFile: ${it?.videoFile}" }
        }
        captureMediaContract?.apply {
            outState.putString(KEY_PICTURE, pictureFile?.path)
            outState.putString(KEY_VIDEO, videoFile?.path)
        }
    }

    /**
     * Initializes the dialog with the style.
     *
     * @param style Style for the dialog.
     */
    fun setStyle(style: MessageInputViewStyle) {
        logger.d { "[setStyle] style: $style" }
        this.style = style
    }

    /**
     * Sets the listener invoked when attachments are selected in the attachment tab.
     */
    fun setAttachmentsPickerTabListener(attachmentsPickerTabListener: AttachmentsPickerTabListener) {
        logger.i { "[setAttachmentsPickerTabListener] attachmentsPickerTabListener: $attachmentsPickerTabListener" }
        this.attachmentsPickerTabListener = attachmentsPickerTabListener
    }

    private fun setupViews() {
        val dialogStyle = style.attachmentSelectionDialogStyle

        binding.grantPermissionsInclude.apply {
            grantPermissionsImageView.setImageDrawable(dialogStyle.allowAccessToCameraIcon)
            grantPermissionsTextView.text = dialogStyle.allowAccessToCameraText
            grantPermissionsTextView.setTextStyle(dialogStyle.grantPermissionsTextStyle)
            grantPermissionsTextView.setOnClickListener {
                checkPermissions()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logger.d { "[onActivityResult] requestCode: $requestCode, resultCode: $resultCode, data: ${data?.stringify()}" }
    }

    private fun setupResultListener() {
        val captureMediaContract = this.captureMediaContract ?: CaptureMediaContract().also {
            this.captureMediaContract = it
        }
        logger.i { "[setupResultListener] captureMediaContract: $captureMediaContract" }
        activityResultLauncher = activity?.activityResultRegistry
            ?.register(LauncherRequestsKeys.CAPTURE_MEDIA, captureMediaContract) { file: File? ->
                logger.i { "[onCameraCallback] file: $file" }
                val result: List<AttachmentMetaData> = if (file == null) {
                    emptyList()
                } else {
                    listOf(AttachmentMetaData(requireContext(), file))
                }

                attachmentsPickerTabListener?.onSelectedAttachmentsChanged(result, AttachmentSource.CAMERA)
                attachmentsPickerTabListener?.onSelectedAttachmentsSubmitted()
            }
    }

    private fun checkPermissions() {
        if (permissionChecker.isNeededToRequestForCameraPermissions(requireContext())) {
            permissionChecker.checkCameraPermissions(
                binding.root,
                onPermissionDenied = ::onPermissionDenied,
                onPermissionGranted = ::onPermissionGranted
            )
            return
        }
        onPermissionGranted()
    }

    private fun onPermissionGranted() {
        binding.grantPermissionsInclude.grantPermissionsContainer.isVisible = false
        activityResultLauncher?.launch(Unit)
    }

    private fun onPermissionDenied() {
        binding.grantPermissionsInclude.grantPermissionsContainer.isVisible = true
    }

    override fun onDestroyView() {
        logger.d { "[onDestroyView] no args" }
        super.onDestroyView()
        activityResultLauncher?.unregister()
        _binding = null
    }

    override fun onDestroy() {
        logger.d { "[onDestroy] no args" }
        super.onDestroy()
    }

    private object LauncherRequestsKeys {
        const val CAPTURE_MEDIA = "capture_media_request_key"
    }

    companion object {
        /**
         * Creates a new instance of [CameraAttachmentFragment].
         *
         * @param style The style for the attachment picker dialog.
         * @return A new instance of the Fragment.
         */
        fun newInstance(style: MessageInputViewStyle): CameraAttachmentFragment {
            return CameraAttachmentFragment().apply {
                setStyle(style)
            }
        }

        private const val KEY_PICTURE = "pictureFile"
        private const val KEY_VIDEO = "videoFile"
    }
}
