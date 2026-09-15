package app.nodenote.worldbuilder

import androidx.room.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nodenote.core.*
import app.nodenote.worldbuilder.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@Dao
interface LegacyDao {
    @Insert suspend fun world(row: WorldRow)

    @Insert suspend fun record(row: RecordRow)

    @Insert suspend fun field(row: FieldRow)

    @Insert suspend fun ref(row: RefRow)
}

@Database(
    entities = [WorldRow::class, RecordRow::class, FieldRow::class, RefRow::class, TimeRow::class],
    version = 1,
    exportSchema = true,
)
abstract class LegacyDatabase : RoomDatabase() {
    abstract fun legacy(): LegacyDao
}

@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    @Test
    fun migrateRealPrefilledVersionOneDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-${newId()}.db"
        val world = World(name = "Migration fixture")
        val entry =
            Record(
                world = world.id,
                kind = Kind.NOTE,
                title = "Before migration",
                body = "Łódź 王朝 😀".repeat(10000),
            )
        val old = Room.databaseBuilder(context, LegacyDatabase::class.java, name).build()
        old.legacy().world(world.row())
        old.legacy().record(entry.row())
        old.legacy().field(FieldRow(entry.id, "aliases", "MigrationAlias"))
        val target = Record(world = world.id, kind = Kind.NOTE, title = "Linked endpoint")
        val relationship =
            Record(
                world = world.id,
                kind = Kind.RELATIONSHIP,
                title = "Preserved relationship",
                body = "Independent relationship notes",
            )
        old.legacy().record(target.row())
        old.legacy().record(relationship.row())
        old.legacy().ref(RefRow(relationship.id, "source", 0, entry.id))
        old.legacy().ref(RefRow(relationship.id, "target", 0, target.id))
        old.close()
        val db =
            Room.databaseBuilder(context, NoteDatabase::class.java, name)
                .addMigrations(NoteDatabase.MIGRATION_1_2)
                .build()
        try {
            val repo = Repository(context, db)
            val migrated = repo.snapshot(world.id).records.associateBy { it.id }
            assertEquals(entry.body, migrated.getValue(entry.id).body)
            assertEquals(entry.id, migrated.getValue(relationship.id).ref("source"))
            assertEquals(target.id, migrated.getValue(relationship.id).ref("target"))
            assertEquals(relationship.body, migrated.getValue(relationship.id).body)
            assertTrue(entry.id in repo.search(world.id, "MigrationAlias"))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
