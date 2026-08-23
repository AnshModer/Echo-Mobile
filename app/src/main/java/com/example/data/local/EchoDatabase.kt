package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val queryText: String,
    val responseText: String,
    val actionType: String, // e.g. "FLASHLIGHT", "VOLUME", "APP_LAUNCH", "AI_CHAT", "NOTE", "TIMER"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_notes")
data class VoiceNoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AssistantDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<CommandHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CommandHistoryItem)

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<VoiceNoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VoiceNoteItem): Long

    @Delete
    suspend fun deleteNote(note: VoiceNoteItem)

    @Query("DELETE FROM voice_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
}

@Database(entities = [CommandHistoryItem::class, VoiceNoteItem::class], version = 1, exportSchema = false)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun assistantDao(): AssistantDao

    companion object {
        @Volatile
        private var INSTANCE: EchoDatabase? = null

        fun getDatabase(context: Context): EchoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoDatabase::class.java,
                    "echo_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
