package com.wapo.flagship.features.audio.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlaylistAudio(vararg playlist: Playlist)

    @Query("Delete from playlist where id=:id")
    abstract suspend fun deletePlaylistAudio(id: String)

    @Query("Select * from playlist where id=:id")
    abstract fun getPlaylistAudio(id: String): Flow<Playlist>

    @Query("Select * from playlist")
    abstract fun getPlaylist(): Flow<List<Playlist>>

    @Query("Delete from playlist")
    abstract suspend fun cleanUp()

    @Query("SELECT EXISTS(SELECT * FROM playlist where id =:id)")
    abstract fun getPlaylistArticalExists(id: String): Boolean
}