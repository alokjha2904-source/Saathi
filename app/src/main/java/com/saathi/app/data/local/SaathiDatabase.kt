package com.saathi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.data.model.TrustedContact

@Database(entities = [TrustedContact::class, EmergencyEvent::class], version = 2, exportSchema = false)
abstract class SaathiDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun emergencyEventDao(): EmergencyEventDao

    companion object {
        @Volatile
        private var INSTANCE: SaathiDatabase? = null

        fun getDatabase(context: Context): SaathiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SaathiDatabase::class.java,
                    "saathi_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
