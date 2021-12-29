package io.getstream.chat.android.compose.ui.channels.info

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.compose.previewdata.PreviewChannelData
import io.getstream.chat.android.compose.previewdata.PreviewUserData
import io.getstream.chat.android.compose.state.channel.list.ChannelAction
import io.getstream.chat.android.compose.ui.components.SimpleMenu
import io.getstream.chat.android.compose.ui.components.channels.ChannelMembers
import io.getstream.chat.android.compose.ui.components.channels.ChannelOptions
import io.getstream.chat.android.compose.ui.components.channels.buildDefaultChannelOptionsState
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.ui.util.getMembersStatusText
import io.getstream.chat.android.compose.ui.util.isOneToOne

/**
 * Shows special UI when an item is selected.
 * It also prepares the available options for the channel, based on if we're an admin or not.
 *
 * @param selectedChannel The channel the user selected.
 * @param isMuted If the channel is muted for the current user.
 * @param currentUser The currently logged-in user data.
 * @param onChannelOptionClick Handler for when the user selects a channel option.
 * @param modifier Modifier for styling.
 * @param shape The shape of the component.
 * @param overlayColor The color applied to the overlay.
 * @param onDismiss Handler called when the dialog is dismissed.
 * @param headerContent The content shown at the top of the dialog.
 * @param centerContent The content shown at the center of the dialog.
 */
@Composable
public fun ChannelInfo(
    selectedChannel: Channel,
    isMuted: Boolean,
    currentUser: User?,
    onChannelOptionClick: (ChannelAction) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ChatTheme.shapes.bottomSheet,
    overlayColor: Color = ChatTheme.colors.overlay,
    onDismiss: () -> Unit = {},
    headerContent: @Composable ColumnScope.() -> Unit = {
        DefaultChannelInfoHeaderContent(
            selectedChannel = selectedChannel,
            currentUser = currentUser
        )
    },
    centerContent: @Composable ColumnScope.() -> Unit = {
        DefaultChannelInfoCenterContent(
            selectedChannel = selectedChannel,
            currentUser = currentUser,
            isMuted = isMuted,
            onChannelOptionClick = onChannelOptionClick
        )
    },
) {
    SimpleMenu(
        modifier = modifier,
        shape = shape,
        overlayColor = overlayColor,
        onDismiss = onDismiss,
        headerContent = headerContent,
        centerContent = centerContent
    )
}

/**
 * Represents the default content shown at the top of [ChannelInfo] dialog.
 *
 * @param selectedChannel The channel the user selected.
 * @param currentUser The currently logged-in user data.
 */
@Composable
internal fun DefaultChannelInfoHeaderContent(
    selectedChannel: Channel,
    currentUser: User?,
) {
    val channelMembers = selectedChannel.members
    val membersToDisplay = if (selectedChannel.isOneToOne(currentUser)) {
        channelMembers.filter { it.user.id != currentUser?.id }
    } else {
        channelMembers
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        textAlign = TextAlign.Center,
        text = ChatTheme.channelNameFormatter.formatChannelName(selectedChannel, currentUser),
        style = ChatTheme.typography.title3Bold,
        color = ChatTheme.colors.textHighEmphasis,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = selectedChannel.getMembersStatusText(LocalContext.current, currentUser),
        style = ChatTheme.typography.footnoteBold,
        color = ChatTheme.colors.textLowEmphasis,
    )

    ChannelMembers(membersToDisplay)
}

/**
 * Represents the default content shown at the center of [ChannelInfo] dialog.
 *
 * @param selectedChannel The channel the user selected.
 * @param currentUser The currently logged-in user data.
 * @param isMuted If the channel is muted for the current user.
 * @param onChannelOptionClick Handler for when the user selects a channel option.
 */
@Composable
internal fun DefaultChannelInfoCenterContent(
    selectedChannel: Channel,
    currentUser: User?,
    isMuted: Boolean,
    onChannelOptionClick: (ChannelAction) -> Unit,
) {
    val channelOptions = buildDefaultChannelOptionsState(
        selectedChannel = selectedChannel,
        currentUser = currentUser,
        isMuted = isMuted,
        channelMembers = selectedChannel.members
    )

    ChannelOptions(channelOptions, onChannelOptionClick)
}

/**
 * Preview of [ChannelInfo] for a channel with many members.
 *
 * Should show a list of channel members and available channel actions.
 */
@Preview(showBackground = true, name = "ChannelInfo Preview")
@Composable
private fun ChannelInfoPreview() {
    ChatTheme {
        ChannelInfo(
            selectedChannel = PreviewChannelData.channelWithManyMembers,
            isMuted = false,
            currentUser = PreviewUserData.user1,
            onChannelOptionClick = {},
        )
    }
}