package com.getfit.data

import androidx.room.Room
import com.getfit.data.db.Curated
import com.getfit.data.db.GetFitDatabase
import com.getfit.data.db.Seeder
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetEntity
import com.getfit.domain.DAY_MS
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedTest {

    private fun db(): GetFitDatabase = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GetFitDatabase::class.java)
        .allowMainThreadQueries().build()

    @Test
    fun seeds_the_library_only_no_demo_history_or_targets() = runBlocking {
        val ctx = RuntimeEnvironment.getApplication()
        val db = db()
        Seeder.seed(ctx, db, 100L * DAY_MS)

        // 1,324 dataset + 18 curated
        assertThat(db.exerciseDao().count()).isEqualTo(1324 + 18)
        assertThat(db.exerciseDao().byId("bench")).isNotNull()

        // A fresh install starts empty: no placeholder PRs, no demo targets.
        assertThat(db.logDao().allOnce()).isEmpty()
        assertThat(db.targetDao().allOnce()).isEmpty()
        assertThat(db.sessionDao().allOnce()).isEmpty()

        // a dataset exercise mapped correctly
        val first = db.exerciseDao().byId("0001")!!
        assertThat(first.muscle).isEqualTo("Core") // body_part "waist"
        assertThat(first.name).isEqualTo("3/4 Sit-up")

        db.close()
    }

    /** Upgraded installs: the old seeded rows go, the user's own rows stay. */
    @Test
    fun purge_removes_only_the_old_demo_rows() = runBlocking {
        val db = db()
        val now = 100L * DAY_MS
        // What Seeder used to insert.
        val demoLogs = Curated.SEED.flatMap { (id, sets) ->
            sets.map { (w, reps, daysAgo) -> SetLogEntity(exerciseId = id, weight = w, reps = reps, dateMs = now - daysAgo * DAY_MS) }
        }
        db.logDao().insertAll(demoLogs)
        db.targetDao().insertAll(
            Curated.SEED_TARGETS.map { TargetEntity(it.id, it.exId, it.target, it.start, now - it.startedDaysAgo * DAY_MS, it.weeks) },
        )
        // The user's own: a session with a log stamped on its date (same values as a demo row on
        // purpose — the session is what makes it real), and a target of their own.
        val sessionMs = now - 4 * DAY_MS + 12_345
        db.sessionDao().insertSession(SessionEntity("h1", sessionMs, "Push Day", 1800, 1, 500, 0))
        db.logDao().insertAll(listOf(SetLogEntity(exerciseId = "bench", weight = 100.0, reps = 5, dateMs = sessionMs)))
        db.targetDao().insert(TargetEntity("t99", "ohp", 70.0, 60.0, now, 8))

        val removed = Seeder.purgeDemoData(db)

        assertThat(removed).isEqualTo(demoLogs.size + Curated.SEED_TARGETS.size)
        val logs = db.logDao().allOnce()
        assertThat(logs).hasSize(1)
        assertThat(logs.single().dateMs).isEqualTo(sessionMs)
        assertThat(db.targetDao().allOnce().map { it.id }).containsExactly("t99")

        // Idempotent: a second pass finds nothing.
        assertThat(Seeder.purgeDemoData(db)).isEqualTo(0)
        db.close()
    }
}
