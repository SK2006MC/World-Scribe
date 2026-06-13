package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "simulation_worlds")
data class SimulationWorld(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String, // Overall summary or starting premise
    val genre: String, // "Fantasy", "Sci-Fi", "Cyberpunk", "Modern-Day", etc.
    val isLocalActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "world_characters",
    foreignKeys = [
        ForeignKey(
            entity = SimulationWorld::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worldId"])]
)
data class WorldCharacter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val name: String,
    val role: String, // "villain", "heroine", "friend", "neutral"
    val status: String = "ALIVE", // "ALIVE" or "DEAD"
    val description: String,
    val historySummary: String = "", // Historic summaries when dead, or backstory
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "world_places",
    foreignKeys = [
        ForeignKey(
            entity = SimulationWorld::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worldId"])]
)
data class WorldPlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val name: String,
    val description: String,
    val isDiscovered: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "world_lore",
    foreignKeys = [
        ForeignKey(
            entity = SimulationWorld::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worldId"])]
)
data class WorldLore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val keyword: String,
    val description: String,
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "world_messages",
    foreignKeys = [
        ForeignKey(
            entity = SimulationWorld::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worldId"])]
)
data class WorldMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val sender: String, // "PLAYER" or "WORLD" or "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val addedCharacterNames: String? = null,
    val addedPlaceNames: String? = null,
    val addedLoreKeywords: String? = null
)

@Dao
interface SimulationDao {
    // Worlds
    @Query("SELECT * FROM simulation_worlds ORDER BY createdAt DESC")
    fun getAllWorlds(): Flow<List<SimulationWorld>>

    @Query("SELECT * FROM simulation_worlds WHERE id = :id LIMIT 1")
    suspend fun getWorldById(id: Long): SimulationWorld?

    @Query("SELECT * FROM simulation_worlds ORDER BY id DESC LIMIT 1")
    suspend fun getLatestActiveWorld(): SimulationWorld?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: SimulationWorld): Long

    @Delete
    suspend fun deleteWorld(world: SimulationWorld)

    // Characters
    @Query("SELECT * FROM world_characters WHERE worldId = :worldId ORDER BY createdAt ASC")
    fun getCharactersForWorld(worldId: Long): Flow<List<WorldCharacter>>

    @Query("SELECT * FROM world_characters WHERE worldId = :worldId")
    suspend fun getCharactersListForWorld(worldId: Long): List<WorldCharacter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: WorldCharacter): Long

    @Update
    suspend fun updateCharacter(character: WorldCharacter)

    @Delete
    suspend fun deleteCharacter(character: WorldCharacter)

    // Places
    @Query("SELECT * FROM world_places WHERE worldId = :worldId ORDER BY createdAt ASC")
    fun getPlacesForWorld(worldId: Long): Flow<List<WorldPlace>>

    @Query("SELECT * FROM world_places WHERE worldId = :worldId")
    suspend fun getPlacesListForWorld(worldId: Long): List<WorldPlace>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: WorldPlace): Long

    @Update
    suspend fun updatePlace(place: WorldPlace)

    // Lore
    @Query("SELECT * FROM world_lore WHERE worldId = :worldId ORDER BY createdAt ASC")
    fun getLoreForWorld(worldId: Long): Flow<List<WorldLore>>

    @Query("SELECT * FROM world_lore WHERE worldId = :worldId")
    suspend fun getLoreListForWorld(worldId: Long): List<WorldLore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLore(lore: WorldLore): Long

    @Update
    suspend fun updateLore(lore: WorldLore)

    // Messages
    @Query("SELECT * FROM world_messages WHERE worldId = :worldId ORDER BY timestamp ASC")
    fun getMessagesForWorld(worldId: Long): Flow<List<WorldMessage>>

    @Query("SELECT * FROM world_messages WHERE worldId = :worldId ORDER BY timestamp ASC")
    suspend fun getMessagesListForWorld(worldId: Long): List<WorldMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: WorldMessage): Long
}

@Database(
    entities = [
        SimulationWorld::class,
        WorldCharacter::class,
        WorldPlace::class,
        WorldLore::class,
        WorldMessage::class
    ],
    version = 2, // Upgraded version for new schema
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract val simulationDao: SimulationDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "mystic_quest_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
