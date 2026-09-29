package com.saathi.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saathi.app.data.model.TrustedContact
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts")
    fun getAllTrustedContacts(): Flow<List<TrustedContact>>

    @Query("SELECT * FROM trusted_contacts")
    fun getAllContactsFlow(): Flow<List<TrustedContact>>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id")
    suspend fun getTrustedContactById(id: Int): TrustedContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedContact(contact: TrustedContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContact)

    @Update
    suspend fun updateTrustedContact(contact: TrustedContact)

    @Update
    suspend fun update(contact: TrustedContact)

    @Delete
    suspend fun deleteTrustedContact(contact: TrustedContact)

    @Delete
    suspend fun delete(contact: TrustedContact)

    @Query("DELETE FROM trusted_contacts WHERE isDemo = 1")
    suspend fun deleteDemoContacts()

    @Query("DELETE FROM trusted_contacts")
    suspend fun deleteAllContacts()
}
