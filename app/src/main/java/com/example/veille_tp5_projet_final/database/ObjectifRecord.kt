package com.example.veille_tp5_projet_final.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "objectif_table")
data class ObjectifRecord (
    @PrimaryKey val date: String,
    var objectif: Int = 6000
)