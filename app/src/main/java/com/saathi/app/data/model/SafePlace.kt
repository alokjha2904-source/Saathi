package com.saathi.app.data.model

data class SafePlace(
    val id: Int,
    val name: String,
    val category: String, // HOSPITAL, POLICE, CAMPUS_SECURITY, PHARMACY, SAFE_LOCATION
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phone: String? = null,
    val distance: Float? = null
) {
    val phoneNumber: String
        get() = phone ?: ""

    val distanceKm: Float
        get() = distance ?: 0f
}
