package app.questling.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A game of any kind: a video game, a board game, a tabletop RPG campaign, a card game, a phone game.
 * [genres] genre ids, comma-separated (domain/Genres.kt). [status] where it stands for the player.
 * [cover] a file in filesDir/covers. [pkg] the Android package of a phone game, for the usage tracker.
 * [earlier] minutes played before Questling (carried over from Steam and the like): in the game's hours, not a session.
 */
@Entity
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: String = VIDEO,
    val platform: String = "",
    val genres: String = "",
    val cover: String = "",
    val status: String = PLAYING,
    val added: Long,
    val finished: Long = 0,
    val rating: Int = 0,                // 0 none, 1..10
    val note: String = "",
    val pkg: String = "",
    val year: Int = 0,
    val maker: String = "",
    val best: Int = 0,                  // the best score ever (board and arcade games)
    @ColumnInfo(defaultValue = "0") val earlier: Int = 0,
) {
    val genreList get() = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    companion object {
        const val VIDEO = "video"; const val BOARD = "board"; const val RPG = "rpg"; const val CARD = "card"; const val MOBILE = "mobile"; const val OTHER = "other"
        const val SHELF = "shelf"; const val PLAYING = "playing"; const val FINISHED = "finished"; const val HUNDRED = "hundred"; const val DROPPED = "dropped"; const val WISH = "wish"
        val KINDS = listOf(VIDEO, BOARD, CARD, RPG, MOBILE, OTHER)
    }
}

/**
 * A play session. [result] how it ended, [score]/[place] the player's own; [seats] keep everybody's.
 * [friends] who played, friend ids comma-separated. [shots] screenshot and photo files, newline-separated.
 * [breaks] breaks taken during it, [auto] found by the phone's usage tracker. [moment] a milestone tag.
 */
@Entity(indices = [Index("day"), Index("gameId")])
data class Play(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val day: Long,
    val start: Long,
    val minutes: Int,
    val platform: String = "",
    val result: Int = NONE,
    val score: Int = Int.MIN_VALUE,
    val place: Int = 0,
    val friends: String = "",
    val mood: Int = 0,                  // 0 not said, 1..5
    val note: String = "",
    val shots: String = "",
    val breaks: Int = 0,
    val auto: Boolean = false,
    val moment: String = "",
    val tools: Int = 0,                 // dice rolls, first-player picks and so on during it
) {
    val shotList get() = shots.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val friendIds get() = friends.split(',').mapNotNull { it.trim().toLongOrNull() }
    val hasScore get() = score != Int.MIN_VALUE
    companion object {
        const val NONE = 0; const val WIN = 1; const val LOSS = 2; const val DRAW = 3; const val COOP_WIN = 4; const val COOP_LOSS = 5
        const val M_FINISH = "finish"; const val M_HUNDRED = "hundred"; const val M_BOSS = "boss"; const val M_FIRST = "first"; const val M_RECORD = "record"; const val M_CHAPTER = "chapter"
    }
}

/** Everybody's score in a session; [friendId] 0 is the player. */
@Entity(indices = [Index("playId")])
data class Seat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playId: Long,
    val friendId: Long,
    val score: Int = Int.MIN_VALUE,
    val place: Int = 0,
    val won: Boolean = false,
)

/** Somebody the player plays with. */
@Entity
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val created: Long,
    val archived: Boolean = false,
    val note: String = "",
)

/**
 * A creature of the tavern. [slot] 0 leads, 1–2 sit by the fire, -1 rests in the menagerie. [growth] counts towards
 * hatching while it's an egg ([hatched] 0), then [xp] grows it. Play fills [belly]; it turns into xp at night.
 * Six stats: might, wits, agility, charm, grit, luck. [genres] «rpg:120,euro:40» – what it was raised on.
 */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val xp: Int = 0,
    val growth: Int = 0,
    val found: Long,
    val hatched: Long = 0,
    val slot: Int = -1,
    val shiny: Boolean = false,
    val belly: Int = 0,
    val str: Int = 0, val wit: Int = 0, val dex: Int = 0, val cha: Int = 0, val wil: Int = 0, val lck: Int = 0,
    val genres: String = "",
) {
    fun genreMap(): Map<String, Int> = genres.split(',').mapNotNull { p -> p.split(':').takeIf { it.size == 2 }?.let { it[0] to (it[1].toIntOrNull() ?: 0) } }.toMap()
    fun withGenre(g: String, n: Int): Pet { val m = genreMap().toMutableMap(); m[g] = (m[g] ?: 0) + n; return copy(genres = m.entries.joinToString(",") { "${it.key}:${it.value}" }) }
    val stats get() = listOf(str, wit, dex, cha, wil, lck)
}

@Dao
interface Dao0 {
    @Query("SELECT * FROM Game ORDER BY title COLLATE NOCASE") fun games(): Flow<List<Game>>
    @Query("SELECT * FROM Game") suspend fun gameList(): List<Game>
    @Insert suspend fun insert(g: Game): Long
    @Update suspend fun update(g: Game)
    @Query("DELETE FROM Game WHERE id = :id") suspend fun deleteGame(id: Long)

    @Query("SELECT * FROM Play ORDER BY start DESC") fun plays(): Flow<List<Play>>
    @Query("SELECT * FROM Play ORDER BY start") suspend fun playList(): List<Play>
    @Insert suspend fun insert(p: Play): Long
    @Update suspend fun update(p: Play)
    @Query("DELETE FROM Play WHERE id = :id") suspend fun deletePlay(id: Long)
    @Query("DELETE FROM Play WHERE gameId = :id") suspend fun deletePlaysOf(id: Long)

    @Query("SELECT * FROM Seat") fun seats(): Flow<List<Seat>>
    @Query("SELECT * FROM Seat") suspend fun seatList(): List<Seat>
    @Insert suspend fun insertSeats(s: List<Seat>)
    @Query("DELETE FROM Seat WHERE playId = :id") suspend fun deleteSeatsOf(id: Long)

    @Query("SELECT * FROM Friend ORDER BY name COLLATE NOCASE") fun friends(): Flow<List<Friend>>
    @Query("SELECT * FROM Friend") suspend fun friendList(): List<Friend>
    @Insert suspend fun insert(f: Friend): Long
    @Update suspend fun update(f: Friend)

    @Query("SELECT * FROM Pet ORDER BY id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("DELETE FROM Game") suspend fun wipeGames()
    @Query("DELETE FROM Play") suspend fun wipePlays()
    @Query("DELETE FROM Seat") suspend fun wipeSeats()
    @Query("DELETE FROM Friend") suspend fun wipeFriends()
    @Query("DELETE FROM Pet") suspend fun wipePets()
}

@Database(entities = [Game::class, Play::class, Seat::class, Friend::class, Pet::class], version = 2)
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        /** 2: the hours played before Questling, on the game (#5). */
        private val TO_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL("ALTER TABLE Game ADD COLUMN earlier INTEGER NOT NULL DEFAULT 0")
        }
        fun create(ctx: Context): Db = Room.databaseBuilder(ctx, Db::class.java, "questling.db").addMigrations(TO_2).build()
    }
}
