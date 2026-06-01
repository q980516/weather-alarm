package cn.codex.weatheralarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDecisionDao {
    @Query("SELECT * FROM weather_decisions WHERE profileId = :profileId ORDER BY checkedAt DESC LIMIT 1")
    fun observeLatest(profileId: Long): Flow<WeatherDecisionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(decision: WeatherDecisionEntity)
}
