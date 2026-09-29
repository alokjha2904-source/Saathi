package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_contacts")
data class TrustedContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val relationship: String,
    val isPrimary: Boolean = false,
    val notifyOnSOS: Boolean = true,
    val notifyOnCheckIn: Boolean = false,
    val receiveLiveLocation: Boolean = true,
    val isDemo: Boolean = false
)
