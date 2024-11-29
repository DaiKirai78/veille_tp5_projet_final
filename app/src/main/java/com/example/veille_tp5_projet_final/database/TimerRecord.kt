package com.example.veille_tp5_projet_final.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_record")
data class TimerRecord(
    @PrimaryKey val date: String,
    val timeElapsed: Long
)