package com.example.veille_tp5_projet_final.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StepDao {
    @Query("SELECT * FROM steps_table ORDER BY date DESC")
    suspend fun getAllSteps(): List<StepRecord>

    @Query("SELECT * FROM steps_table WHERE date = :date LIMIT 1")
    suspend fun getStepsForDate(date: String): StepRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStep(stepRecord: StepRecord)

    @Query("SELECT objectif FROM objectif_table WHERE date = :date LIMIT 1")
    suspend fun getObjectifForDate(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateObjectif(objectifRecord: ObjectifRecord)

    @Query("SELECT * FROM timer_record WHERE date = :date LIMIT 1")
    suspend fun getTimerForDate(date: String): TimerRecord?

    @Query("SELECT * FROM timer_record ORDER BY date DESC")
    suspend fun getAllTimers(): List<TimerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTimer(timerRecord: TimerRecord)
}