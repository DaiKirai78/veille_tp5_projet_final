package com.example.veille_tp5_projet_final.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps_table")
data class StepRecord(
    @PrimaryKey val date: String,
    var steps: Int,
    var isRunning: Boolean
)