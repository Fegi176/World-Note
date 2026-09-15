package app.nodenote.worldbuilder.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "worlds")
data class WorldRow(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val archived: Boolean,
    val origin: String,
    val present: String,
    val epochLabel: String,
    val eraLabel: String,
    val created: Long,
)

@Entity(
    tableName = "records",
    foreignKeys =
        [
            ForeignKey(
                entity = WorldRow::class,
                parentColumns = ["id"],
                childColumns = ["world"],
                onDelete = ForeignKey.RESTRICT,
            )
        ],
    indices = [Index("world"), Index(value = ["world", "kind"]), Index("updated")],
)
data class RecordRow(
    @PrimaryKey val id: String,
    val world: String,
    val kind: String,
    val title: String,
    val summary: String,
    val body: String,
    val canon: String,
    val writing: String,
    val favorite: Boolean,
    val inbox: Boolean,
    val locked: Boolean,
    val trashed: Boolean,
    val revision: Long,
    val created: Long,
    val updated: Long,
)

@Entity(
    tableName = "fields",
    primaryKeys = ["owner", "key"],
    foreignKeys =
        [
            ForeignKey(
                entity = RecordRow::class,
                parentColumns = ["id"],
                childColumns = ["owner"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class FieldRow(val owner: String, val key: String, val value: String)

@Entity(
    tableName = "refs",
    primaryKeys = ["owner", "role", "ordinal"],
    foreignKeys =
        [
            ForeignKey(
                entity = RecordRow::class,
                parentColumns = ["id"],
                childColumns = ["owner"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = RecordRow::class,
                parentColumns = ["id"],
                childColumns = ["target"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices = [Index("target")],
)
data class RefRow(val owner: String, val role: String, val ordinal: Int, val target: String)

@Entity(
    tableName = "times",
    primaryKeys = ["owner", "slot"],
    foreignKeys =
        [
            ForeignKey(
                entity = RecordRow::class,
                parentColumns = ["id"],
                childColumns = ["owner"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class TimeRow(val owner: String, val slot: String, val expression: String)

@Fts4
@Entity(tableName = "search_index")
data class SearchRow(val recordId: String, val world: String, val content: String)

data class RecordIdentity(val id: String, val world: String)

@Dao
interface NoteDao {
    @Query("SELECT * FROM worlds ORDER BY created") fun worlds(): Flow<List<WorldRow>>

    @Query("SELECT * FROM worlds ORDER BY created") suspend fun allWorlds(): List<WorldRow>

    @Query("SELECT * FROM records WHERE world=:world ORDER BY updated DESC")
    fun observeRecords(world: String): Flow<List<RecordRow>>

    @Query("SELECT * FROM records WHERE world=:world")
    suspend fun records(world: String): List<RecordRow>

    @Query("SELECT id FROM records WHERE world=:world")
    suspend fun recordIds(world: String): List<String>

    @Query("SELECT * FROM records WHERE id IN (:ids)")
    suspend fun recordsByIds(ids: List<String>): List<RecordRow>

    @Query("SELECT * FROM fields WHERE owner IN (:ids)")
    suspend fun fieldsByIds(ids: List<String>): List<FieldRow>

    @Query("SELECT * FROM refs WHERE owner IN (:ids)")
    suspend fun refsByIds(ids: List<String>): List<RefRow>

    @Query("SELECT * FROM times WHERE owner IN (:ids)")
    suspend fun timesByIds(ids: List<String>): List<TimeRow>

    @Query("SELECT * FROM records WHERE id=:id") suspend fun record(id: String): RecordRow?

    @Query("SELECT id,world FROM records WHERE id IN (:ids)")
    suspend fun identities(ids: List<String>): List<RecordIdentity>

    @Query("SELECT id FROM records WHERE kind='ATTACHMENT'")
    suspend fun attachmentIds(): List<String>

    @Query(
        "SELECT records.* FROM records JOIN refs ON refs.owner=records.id WHERE records.world=:world AND records.kind=:kind AND refs.role='owner' AND refs.target=:owner"
    )
    suspend fun owned(world: String, owner: String, kind: String): List<RecordRow>

    @Query("SELECT * FROM fields WHERE owner=:id")
    suspend fun recordFields(id: String): List<FieldRow>

    @Query("SELECT * FROM refs WHERE owner=:id") suspend fun recordRefs(id: String): List<RefRow>

    @Query("SELECT * FROM times WHERE owner=:id") suspend fun recordTimes(id: String): List<TimeRow>

    @Query(
        "SELECT fields.* FROM fields JOIN records ON records.id=fields.owner WHERE records.world=:world"
    )
    suspend fun fields(world: String): List<FieldRow>

    @Query(
        "SELECT refs.* FROM refs JOIN records ON records.id=refs.owner WHERE records.world=:world"
    )
    suspend fun refs(world: String): List<RefRow>

    @Query(
        "SELECT times.* FROM times JOIN records ON records.id=times.owner WHERE records.world=:world"
    )
    suspend fun times(world: String): List<TimeRow>

    @Upsert suspend fun world(row: WorldRow)

    @Upsert suspend fun records(rows: List<RecordRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun fields(rows: List<FieldRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun refs(rows: List<RefRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun times(rows: List<TimeRow>)

    @Insert suspend fun index(row: SearchRow)

    @Insert suspend fun indexes(rows: List<SearchRow>)

    @Query("DELETE FROM fields WHERE owner IN (:ids)") suspend fun clearFieldsFor(ids: List<String>)

    @Query("DELETE FROM refs WHERE owner IN (:ids)") suspend fun clearRefsFor(ids: List<String>)

    @Query("DELETE FROM times WHERE owner IN (:ids)") suspend fun clearTimesFor(ids: List<String>)

    @Query("DELETE FROM search_index WHERE recordId IN (:ids)")
    suspend fun clearIndexesFor(ids: List<String>)

    @Query("DELETE FROM refs WHERE owner IN (SELECT id FROM records WHERE world=:world)")
    suspend fun clearWorldRefs(world: String)

    @Query("DELETE FROM search_index WHERE world=:world") suspend fun clearWorldIndex(world: String)

    @Query("DELETE FROM records WHERE world=:world") suspend fun deleteWorldRecords(world: String)

    @Query("DELETE FROM fields WHERE owner=:id") suspend fun clearFields(id: String)

    @Query("DELETE FROM refs WHERE owner=:id") suspend fun clearRefs(id: String)

    @Query("DELETE FROM times WHERE owner=:id") suspend fun clearTimes(id: String)

    @Query("DELETE FROM search_index WHERE recordId=:id") suspend fun clearIndex(id: String)

    @Query("DELETE FROM records WHERE id=:id") suspend fun deleteRecord(id: String)

    @Query("DELETE FROM worlds WHERE id=:id") suspend fun deleteWorld(id: String)

    @Query("SELECT recordId FROM search_index WHERE world=:world AND search_index MATCH :query")
    suspend fun search(world: String, query: String): List<String>

    @Query(
        "SELECT recordId FROM search_index WHERE world=:world AND content LIKE :query ESCAPE '\\'"
    )
    suspend fun substring(world: String, query: String): List<String>
}

@Database(
    entities =
        [
            WorldRow::class,
            RecordRow::class,
            FieldRow::class,
            RefRow::class,
            TimeRow::class,
            SearchRow::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun dao(): NoteDao

    internal val writeMutex = kotlinx.coroutines.sync.Mutex()
    internal val startupMutex = kotlinx.coroutines.sync.Mutex()
    internal var startupRecovered = false
    // Process-local generations track normalized child changes even when authored revision is
    // unchanged.
    internal val recordVersions = java.util.concurrent.ConcurrentHashMap<String, Long>()
    internal val changeVersion = java.util.concurrent.atomic.AtomicLong()

    companion object {
        @Volatile private var instance: NoteDatabase? = null

        fun get(context: android.content.Context): NoteDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: androidx.room.Room.databaseBuilder(
                                context.applicationContext,
                                NoteDatabase::class.java,
                                "worldbuilder.db",
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build()
                            .also { instance = it }
                }

        // v1 already contained authored normalized records. v2 adds a rebuildable search index
        // only.
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE VIRTUAL TABLE IF NOT EXISTS `search_index` USING FTS4(`recordId` TEXT NOT NULL, `world` TEXT NOT NULL, `content` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO search_index(recordId,world,content) SELECT id,world,title || ' ' || summary || ' ' || body || ' ' || COALESCE((SELECT group_concat(value,' ') FROM fields WHERE owner=records.id),'') FROM records"
                    )
                }
            }
    }
}
