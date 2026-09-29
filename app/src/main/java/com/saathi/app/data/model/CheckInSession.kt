package com.saathi.app.data.model

data class CheckInSession(
    val id: String,
    val origin: String,
    val destination: String,
    val startTime: Long,
    val expectedArrivalTime: Long,
    val isActive: Boolean = true,
    val checkedIn: Boolean = false
)
