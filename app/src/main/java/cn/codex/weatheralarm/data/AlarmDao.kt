package cn.codex.weatheralarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_profiles WHERE id = :id")
    fun observeProfile(id: Long): Flow<AlarmProfileEntity?>

    @Query("SELECT * FROM alarm_profiles WHERE id = :id")
    suspend fun getProfile(id: Long): AlarmProfileEntity?

    @Query("SELECT * FROM alarm_profiles WHERE enabled = 1")
    suspend fun getEnabledProfiles(): List<AlarmProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: AlarmProfileEntity)
}
