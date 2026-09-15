package app.nodenote.worldbuilder

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nodenote.core.*
import app.nodenote.worldbuilder.data.*
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerformanceInstrumentedTest {
    @Test
    fun typicalWorldIndexedSearchAndSnapshot() = measureWorld(false)

    @Test
    fun stressWorldIndexedSearchAndSnapshot() = measureWorld(true)

    private fun measureWorld(stress: Boolean) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        val repo = Repository(context, db)
        try {
            val fixture = if (stress) Fixtures.world(10000, 25000, 2000) else Fixtures.world()
            val insert = measureTimeMillis { repo.insert(fixture) }
            var snapshot: WorldBundle? = null
            val read = measureTimeMillis { snapshot = repo.snapshot(fixture.world.id) }
            val search = measureTimeMillis {
                assertTrue(repo.search(fixture.world.id, "relation-4321").isNotEmpty())
            }
            val chronology = measureTimeMillis {
                Chronology(snapshot!!.records)
                    .ordered(snapshot!!.records.filter { it.kind == Kind.EVENT })
            }
            Log.i(
                "NodeNotePerformance",
                "records=${fixture.records.size} insertMs=$insert snapshotMs=$read searchMs=$search chronologyMs=$chronology device=${android.os.Build.MODEL}-API${android.os.Build.VERSION.SDK_INT} debug inMemorySQLite=true stress=$stress",
            )
            assertEquals(fixture.records.size, snapshot!!.records.size)
        } finally {
            db.close()
        }
    }
}
