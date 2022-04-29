package io.getstream.chat.android.offline.repository.domain.channel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.getstream.chat.android.client.utils.SyncStatus
import java.util.Date

@Dao
public interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(channelEntity: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertMany(channelEntities: List<ChannelEntity>)

    @Transaction
    @Query("SELECT cid FROM stream_chat_channel_state")
    public suspend fun selectAllCids(): List<String>

    @Query("""
            SELECT cid FROM stream_chat_channel_state 
                WHERE syncStatus = :syncStatus ORDER BY syncStatus ASC LIMIT :limit
        """)
    public suspend fun selectCidsSyncsNeeded(
        syncStatus: SyncStatus = SyncStatus.SYNC_NEEDED,
        limit: Int = -1,
    ): List<String>

    @Transaction
    @Query("""
            SELECT * FROM stream_chat_channel_state 
                WHERE syncStatus = :syncStatus ORDER BY syncStatus ASC LIMIT :limit
        """)
    public suspend fun selectSyncNeeded(
        syncStatus: SyncStatus = SyncStatus.SYNC_NEEDED,
        limit: Int = -1,
    ): List<ChannelEntity>

    @Query(
        "SELECT * FROM stream_chat_channel_state " +
            "WHERE stream_chat_channel_state.cid IN (:cids)"
    )
    public suspend fun select(cids: List<String>): List<ChannelEntity>

    @Query(
        "SELECT * FROM stream_chat_channel_state " +
            "WHERE stream_chat_channel_state.cid IN (:cid)"
    )
    public suspend fun select(cid: String?): ChannelEntity?

    @Query("DELETE from stream_chat_channel_state WHERE cid = :cid")
    public suspend fun delete(cid: String)

    @Query("UPDATE stream_chat_channel_state SET deletedAt = :deletedAt WHERE cid = :cid")
    public suspend fun setDeletedAt(cid: String, deletedAt: Date)

    @Query("UPDATE stream_chat_channel_state SET hidden = :hidden, hideMessagesBefore = :hideMessagesBefore WHERE cid = :cid")
    public suspend fun setHidden(cid: String, hidden: Boolean, hideMessagesBefore: Date)

    @Query("UPDATE stream_chat_channel_state SET hidden = :hidden WHERE cid = :cid")
    public suspend fun setHidden(cid: String, hidden: Boolean)

    @Query("DELETE FROM stream_chat_channel_state")
    public suspend fun deleteAll(): Int
}
