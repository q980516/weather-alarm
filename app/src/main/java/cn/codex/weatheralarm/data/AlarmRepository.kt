package cn.codex.weatheralarm.data

import cn.codex.weatheralarm.domain.AlarmProfile
import cn.codex.weatheralarm.domain.WeatherDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val weatherDecisionDao: WeatherDecisionDao
) {
    fun observeProfile(id: Long = AlarmProfile.DEFAULT_ID): Flow<AlarmProfile> =
        alarmDao.observeProfile(id).map { it?.toDomain() ?: AlarmProfile() }

    fun observeLatestDecision(profileId: Long = AlarmProfile.DEFAULT_ID): Flow<WeatherDecision?> =
        weatherDecisionDao.observeLatest(profileId).map { it?.toDomain() }

    suspend fun getLatestDecision(profileId: Long = AlarmProfile.DEFAULT_ID): WeatherDecision? =
        weatherDecisionDao.getLatest(profileId)?.toDomain()

    suspend fun ensureDefaultProfile() {
        if (alarmDao.getProfile(AlarmProfile.DEFAULT_ID) == null) {
            alarmDao.saveProfile(AlarmProfile().toEntity())
        }
    }

    suspend fun getProfile(id: Long = AlarmProfile.DEFAULT_ID): AlarmProfile =
        alarmDao.getProfile(id)?.toDomain() ?: AlarmProfile()

    suspend fun getEnabledProfiles(): List<AlarmProfile> =
        alarmDao.getEnabledProfiles().map { it.toDomain() }

    suspend fun saveProfile(profile: AlarmProfile) {
        alarmDao.saveProfile(profile.toEntity())
    }

    suspend fun saveDecision(decision: WeatherDecision) {
        weatherDecisionDao.save(decision.toEntity())
    }
}
