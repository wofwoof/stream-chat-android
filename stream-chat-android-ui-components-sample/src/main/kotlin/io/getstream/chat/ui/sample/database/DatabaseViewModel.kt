package io.getstream.chat.ui.sample.database

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.getstream.chat.android.client.call.await
import io.getstream.chat.android.client.logger.ChatLogger
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.UserEntity
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.offline.extensions.isPermanent
import io.getstream.chat.android.offline.repository.database.ChatDatabase
import io.getstream.chat.android.offline.repository.database.converter.DateConverter
import io.getstream.chat.android.offline.repository.database.converter.ExtraDataConverter
import io.getstream.chat.android.offline.repository.database.converter.ListConverter
import io.getstream.chat.android.offline.repository.database.converter.MapConverter
import io.getstream.chat.android.offline.repository.database.converter.SetConverter
import io.getstream.chat.android.offline.repository.database.converter.SyncStatusConverter
import io.getstream.chat.android.offline.repository.domain.channel.ChannelEntity
import io.getstream.chat.android.offline.repository.domain.channel.member.MemberEntity
import io.getstream.chat.android.offline.repository.domain.channel.userread.ChannelUserReadEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val TAG = "DatabaseViewModel"

class DatabaseViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DatabaseViewModel(application) as T
    }
}
enum class Database {
    TEST_1, TEST_2
}

class DatabaseViewModel(
    private val application: Application,
) : ViewModel() {


    private val database1 by lazy { ChatDatabase.getDatabase(application, userId = "test1") }
    private val database2 by lazy { ChatDatabase.getDatabase(application, userId = "test2") }
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private val __dateConverter = DateConverter()
    private val __mapConverter = MapConverter()
    private val __listConverter = ListConverter()
    private val __extraDataConverter = ExtraDataConverter()
    private val __syncStatusConverter = SyncStatusConverter()
    private val __setConverter = SetConverter()

    private fun Database.select() = if (this == Database.TEST_1) database1 else database2

    private fun generateString(length: Int): String {
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun onOpen(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onOpen-$traceId] no args")
            val database = type.select()

        }
    }

    fun onClose(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onClose-$traceId] no args")
            val database = type.select()
            val count = database.close()
            logV("[onClose-$traceId] deleted: $count")
        }
    }

    fun onDeleteClick(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onDeleteClick-$traceId] no args")
            val database = type.select()
            val count = database.channelStateDao().deleteAll()
            logV("[onDeleteClick-$traceId] deleted: $count")
        }
    }

    fun onGenerateClick(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onGenerateClick-$traceId] no args")
            val channels = generateChannels(count = 500, memberCount = 100)
            logV("[onGenerateClick-$traceId] generated")
            val database = type.select()
            database.channelStateDao().insertMany(channels)
            logV("[onGenerateClick-$traceId] inserted: ${channels.size}")
        }
    }

    fun onReadClick(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onReadClick-$traceId] no args")
            val database = type.select()
            val result = database.channelStateDao().selectSyncNeeded(SyncStatus.SYNC_NEEDED)
            logV("[onReadClick-$traceId] completed: ${result.size}")
        }
    }

    fun onReadCidsClick(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            val traceId = Random.nextInt(100)
            logD("[onReadCidsClick-$traceId] no args")
            val database = type.select()
            val result = database.channelStateDao().selectCidsSyncsNeeded(SyncStatus.SYNC_NEEDED)
            logV("[onReadCidsClick-$traceId] completed: ${result.size}")
        }
    }

    fun onReadManyClick(type: Database) {
        viewModelScope.launch(Dispatchers.IO) {
            retryChannels(type)

            /*
            val traceId = Random.nextInt(100)
            logD("[onReadManyClick-$traceId] no args")
            val result = (0..10_000).map { index ->
                launch(Dispatchers.IO) {
                    logV("[onReadManyClick-$traceId:$index] switched to IO")
                    val database = type.select()
                    database.query("SELECT * FROM stream_chat_channel_state WHERE stream_chat_channel_state.syncStatus IN (-1)", null).also {
                        CursorLeakObject.cursors["$index"] = it
                        val cursor = TestCursor(it)
                        logV("[onReadManyClick--$traceId:$index] received: ${cursor.count}")
                        simulateChannelDaoImpl(cursor)
                        logV("[onReadManyClick-$traceId:$index] simulation finished")
                    }
                    /*database.channelStateDao().selectSyncNeeded(SyncStatus.SYNC_NEEDED).also {
                        logV("[onReadClick-$traceId:$index] received: ${it.size}")
                    }*/
                }
            }
            logV("[onReadManyClick] completed($traceId): ${result.size}")
             */
        }
    }

    public suspend fun retryChannels(type: Database) {
        val database = type.select()
        logD("[retryChannels] no args")
        val channelLimit = 50
        while (true) {
            val channels = database.channelStateDao().selectSyncNeeded(limit = channelLimit)
            logV("[retryChannels] found: ${channels.size}")
            if (channels.isEmpty()) {
                logW("[retryChannels] completed: ${channels.size}")
                return
            }
            val results = channels.map { channel ->
                viewModelScope.async(Dispatchers.IO) {
                    channel to delay(200L).let {
                        Result.success(channel)
                    }
                }
            }.awaitAll()
            logV("[retryChannels] sent to BE: ${results.size}")

            val toInsert = results.map { (channel, result) ->
                if (result.isSuccess) {
                    channel.copy(syncStatus = SyncStatus.COMPLETED)
                } else if (result.isError && result.error().isPermanent()) {
                    channel.copy(syncStatus = SyncStatus.FAILED_PERMANENTLY)
                } else channel
            }
            database.channelStateDao().insertMany(toInsert)
            logV("[retryChannels] inserted: ${channels.size}")
            if (channels.size < channelLimit) {
                logW("[retryChannels] completed: ${channels.size}")
                return
            }
        }
    }

    private fun generateChannels(count: Int, memberCount: Int): List<ChannelEntity> {
        return (0 until count).map { index ->
            val channelId = "${index}_${generateString(8)}"
            ChannelEntity(
                type = "messaging",
                channelId = channelId,
                cooldown = 0,
                frozen = false,
                createdAt = Date(),
                updatedAt = Date(),
                deletedAt = null,
                extraData = generateExtraData(count = 20),
                syncStatus = SyncStatus.SYNC_NEEDED,
                hidden = false,
                hideMessagesBefore = Date(),
                members = generateMembers(count = memberCount),
                memberCount = 100,
                reads = generateReads(count = 100),
                lastMessageId = generateString(length = 10),
                lastMessageAt = Date(),
                createdByUserId = "createdBy.id",
                watcherIds = listOf(),
                watcherCount = 100,
                team = "team",
                ownCapabilities = setOf(
                    "ban-channel-members",
                    "connect-events",
                    "delete-own-message",
                    "flag-message",
                    "freeze-channel",
                    "join-channel",
                    "leave-channel",
                    "mute-channel",
                    "pin-message",
                    "quote-message",
                    "read-events",
                    "search-messages",
                    "send-custom-events",
                    "send-links",
                    "send-message",
                    "send-reaction",
                    "send-reply",
                    "send-typing-events",
                    "set-channel-cooldown",
                    "typing-events",
                    "update-channel",
                    "update-channel-members",
                    "update-own-message",
                    "upload-file"
                ),
            )
        }
    }

    private fun generateMembers(count: Int): Map<String, MemberEntity> {
        return (0 until count).map { index ->
            MemberEntity(
                userId = "${index}_${generateString(8)}",
                role = "member",
                createdAt = Date(),
                updatedAt = Date(),
                isInvited = false,
                inviteAcceptedAt = null,
                inviteRejectedAt = null,
                shadowBanned = false,
                banned = false,
                channelRole = "chat_member"
            )
        }.associateBy { it.userId }
    }

    private fun generateReads(count: Int): Map<String, ChannelUserReadEntity> {
        return (0 until count).map { index ->
            ChannelUserReadEntity(
                userId = "${index}_${generateString(8)}",
                lastRead = Date(),
                unreadMessages = Random.nextInt(until = 100),
                lastMessageSeenDate = Date()
            )
        }.associateBy { it.userId }
    }

    private fun generateExtraData(count: Int): Map<String, Any> {
        return (0 until count).associate { index ->
            "${index}_${generateString(length = 8)}" to generateString(length = 20)
        }
    }

    private fun logD(message: String) {
        val thread = Thread.currentThread().run { "${id}:${name}" }
        Log.d(TAG, "($thread) $message")
    }

    private fun logV(message: String) {
        val thread = Thread.currentThread().run { "${id}:${name}" }
        Log.v(TAG, "($thread) $message")
    }

    private fun logW(message: String) {
        val thread = Thread.currentThread().run { "${id}:${name}" }
        Log.w(TAG, "($thread) $message")
    }

    private fun simulateChannelDaoImpl(_cursor: Cursor) {
        val _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type")
        val _cursorIndexOfChannelId = _cursor.getColumnIndexOrThrow("channelId")
        val _cursorIndexOfCooldown = _cursor.getColumnIndexOrThrow("cooldown")
        val _cursorIndexOfCreatedByUserId = _cursor.getColumnIndexOrThrow("createdByUserId")
        val _cursorIndexOfFrozen = _cursor.getColumnIndexOrThrow("frozen")
        val _cursorIndexOfHidden = _cursor.getColumnIndexOrThrow("hidden")
        val _cursorIndexOfHideMessagesBefore = _cursor.getColumnIndexOrThrow("hideMessagesBefore")
        val _cursorIndexOfMembers = _cursor.getColumnIndexOrThrow("members")
        val _cursorIndexOfMemberCount = _cursor.getColumnIndexOrThrow("memberCount")
        val _cursorIndexOfWatcherIds = _cursor.getColumnIndexOrThrow("watcherIds")
        val _cursorIndexOfWatcherCount = _cursor.getColumnIndexOrThrow("watcherCount")
        val _cursorIndexOfReads = _cursor.getColumnIndexOrThrow("reads")
        val _cursorIndexOfLastMessageAt = _cursor.getColumnIndexOrThrow("lastMessageAt")
        val _cursorIndexOfLastMessageId = _cursor.getColumnIndexOrThrow("lastMessageId")
        val _cursorIndexOfCreatedAt = _cursor.getColumnIndexOrThrow("createdAt")
        val _cursorIndexOfUpdatedAt = _cursor.getColumnIndexOrThrow("updatedAt")
        val _cursorIndexOfDeletedAt = _cursor.getColumnIndexOrThrow("deletedAt")
        val _cursorIndexOfExtraData = _cursor.getColumnIndexOrThrow("extraData")
        val _cursorIndexOfSyncStatus = _cursor.getColumnIndexOrThrow("syncStatus")
        val _cursorIndexOfTeam = _cursor.getColumnIndexOrThrow("team")
        val _cursorIndexOfOwnCapabilities = _cursor.getColumnIndexOrThrow("ownCapabilities")
        val _cursorIndexOfCid = _cursor.getColumnIndexOrThrow("cid")
        val _result: ArrayList<ChannelEntity> = ArrayList(_cursor.count)

        while (_cursor.moveToNext()) {
            val _item: ChannelEntity
            val _tmpType: String?
            _tmpType = if (_cursor.isNull(_cursorIndexOfType)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfType)
            }
            val _tmpChannelId: String?
            _tmpChannelId = if (_cursor.isNull(_cursorIndexOfChannelId)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfChannelId)
            }
            val _tmpCooldown: Int
            _tmpCooldown = _cursor.getInt(_cursorIndexOfCooldown)
            val _tmpCreatedByUserId: String?
            _tmpCreatedByUserId = if (_cursor.isNull(_cursorIndexOfCreatedByUserId)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfCreatedByUserId)
            }
            val _tmpFrozen: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfFrozen)
            _tmpFrozen = _tmp_1 != 0
            val _tmpHidden: Boolean?
            val _tmp_2: Int?
            _tmp_2 = if (_cursor.isNull(_cursorIndexOfHidden)) {
                null
            } else {
                _cursor.getInt(_cursorIndexOfHidden)
            }
            _tmpHidden = if (_tmp_2 == null) null else _tmp_2 != 0
            val _tmpHideMessagesBefore: Date?
            val _tmp_3: Long?
            _tmp_3 = if (_cursor.isNull(_cursorIndexOfHideMessagesBefore)) {
                null
            } else {
                _cursor.getLong(_cursorIndexOfHideMessagesBefore)
            }
            _tmpHideMessagesBefore = __dateConverter.fromTimestamp(_tmp_3)
            val _tmpMembers: Map<String, MemberEntity>?
            val _tmp_4: String?
            _tmp_4 = if (_cursor.isNull(_cursorIndexOfMembers)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfMembers)
            }
            _tmpMembers = __mapConverter.stringToMemberMap(_tmp_4)
            val _tmpMemberCount: Int
            _tmpMemberCount = _cursor.getInt(_cursorIndexOfMemberCount)
            val _tmpWatcherIds: List<String>?
            val _tmp_5: String?
            _tmp_5 = if (_cursor.isNull(_cursorIndexOfWatcherIds)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfWatcherIds)
            }
            _tmpWatcherIds = __listConverter.stringToStringList(_tmp_5)
            val _tmpWatcherCount: Int
            _tmpWatcherCount = _cursor.getInt(_cursorIndexOfWatcherCount)
            val _tmpReads: Map<String, ChannelUserReadEntity>?
            val _tmp_6: String?
            _tmp_6 = if (_cursor.isNull(_cursorIndexOfReads)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfReads)
            }
            _tmpReads = __mapConverter.stringToReadMap(_tmp_6)
            val _tmpLastMessageAt: Date?
            val _tmp_7: Long?
            _tmp_7 = if (_cursor.isNull(_cursorIndexOfLastMessageAt)) {
                null
            } else {
                _cursor.getLong(_cursorIndexOfLastMessageAt)
            }
            _tmpLastMessageAt = __dateConverter.fromTimestamp(_tmp_7)
            val _tmpLastMessageId: String?
            _tmpLastMessageId = if (_cursor.isNull(_cursorIndexOfLastMessageId)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfLastMessageId)
            }
            val _tmpCreatedAt: Date?
            val _tmp_8: Long?
            _tmp_8 = if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                null
            } else {
                _cursor.getLong(_cursorIndexOfCreatedAt)
            }
            _tmpCreatedAt = __dateConverter.fromTimestamp(_tmp_8)
            val _tmpUpdatedAt: Date?
            val _tmp_9: Long?
            _tmp_9 = if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                null
            } else {
                _cursor.getLong(_cursorIndexOfUpdatedAt)
            }
            _tmpUpdatedAt = __dateConverter.fromTimestamp(_tmp_9)
            val _tmpDeletedAt: Date?
            val _tmp_10: Long?
            _tmp_10 = if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
                null
            } else {
                _cursor.getLong(_cursorIndexOfDeletedAt)
            }
            _tmpDeletedAt = __dateConverter.fromTimestamp(_tmp_10)
            val _tmpExtraData: Map<String, Any>?
            val _tmp_11: String?
            _tmp_11 = if (_cursor.isNull(_cursorIndexOfExtraData)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfExtraData)
            }
            _tmpExtraData = __extraDataConverter.stringToMap(_tmp_11)
            val _tmpSyncStatus: SyncStatus
            val _tmp_12: Int
            _tmp_12 = _cursor.getInt(_cursorIndexOfSyncStatus)
            _tmpSyncStatus = __syncStatusConverter.stringToSyncStatus(_tmp_12)
            val _tmpTeam: String?
            _tmpTeam = if (_cursor.isNull(_cursorIndexOfTeam)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfTeam)
            }
            val _tmpOwnCapabilities: Set<String>?
            val _tmp_13: String?
            _tmp_13 = if (_cursor.isNull(_cursorIndexOfOwnCapabilities)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfOwnCapabilities)
            }
            _tmpOwnCapabilities = __setConverter.stringToSortedSet(_tmp_13)
            _item = ChannelEntity(_tmpType!!,
                _tmpChannelId!!,
                _tmpCooldown,
                _tmpCreatedByUserId!!,
                _tmpFrozen,
                _tmpHidden,
                _tmpHideMessagesBefore,
                _tmpMembers!!,
                _tmpMemberCount,
                _tmpWatcherIds!!,
                _tmpWatcherCount,
                _tmpReads!!,
                _tmpLastMessageAt,
                _tmpLastMessageId,
                _tmpCreatedAt,
                _tmpUpdatedAt,
                _tmpDeletedAt,
                _tmpExtraData!!,
                _tmpSyncStatus,
                _tmpTeam!!,
                _tmpOwnCapabilities!!)
            val _tmpCid: String?
            _tmpCid = if (_cursor.isNull(_cursorIndexOfCid)) {
                null
            } else {
                _cursor.getString(_cursorIndexOfCid)
            }
            _item.cid = _tmpCid!!
            _result.add(_item)
        }
    }
}

object CursorLeakObject {
    val cursors = ConcurrentHashMap<String, Cursor>()
}