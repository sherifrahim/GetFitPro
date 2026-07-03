package com.getfit.data

import androidx.room.Room
import com.getfit.data.db.GetFitDatabase
import com.getfit.data.db.Seeder
import com.getfit.domain.DAY_MS
import com.getfit.domain.LoggedSet
import com.getfit.domain.bestFor
import com.getfit.domain.isBW
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

    @Test
    fun seeds_full_library_plus_curated_and_derives_bench_pr() = runBlocking {
        val ctx = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(ctx, GetFitDatabase::class.java)
            .allowMainThreadQueries().build()

        val now = 100L * DAY_MS
        Seeder.seed(ctx, db, now)

        // 1,324 dataset + 18 curated
        assertThat(db.exerciseDao().count()).isEqualTo(1324 + 18)

        // curated bench exists and best-of-seed = 100kg, e1rm 117
        val bench = db.exerciseDao().byId("bench")!!
        val logs = db.logDao().forExercise("bench")
            .map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }
        val best = bestFor(logs, isBW(bench.equipment, bench.reps))!!
        assertThat(best.weight).isEqualTo(100.0)
        assertThat(best.e1rm).isEqualTo(117)

        // a dataset exercise mapped correctly
        val first = db.exerciseDao().byId("0001")!!
        assertThat(first.muscle).isEqualTo("Core") // body_part "waist"
        assertThat(first.name).isEqualTo("3/4 Sit-up")

        db.close()
    }
}
