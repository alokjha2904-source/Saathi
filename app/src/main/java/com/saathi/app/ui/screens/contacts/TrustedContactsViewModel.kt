package com.saathi.app.ui.screens.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.TrustedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TrustedContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = SaathiDatabase.getDatabase(application).trustedContactDao()

    private val _contacts = MutableStateFlow<List<TrustedContact>>(emptyList())
    val contacts: StateFlow<List<TrustedContact>> = _contacts.asStateFlow()

    init {
        viewModelScope.launch {
            contactDao.getAllTrustedContacts()
                .catch { e ->
                    e.printStackTrace()
                }
                .collect {
                    _contacts.value = it
                }
        }
    }

    fun addContact(contact: TrustedContact) {
        if (!validateContact(contact)) return
        viewModelScope.launch {
            contactDao.insert(contact)
        }
    }

    fun updateContact(contact: TrustedContact) {
        if (!validateContact(contact)) return
        viewModelScope.launch {
            contactDao.update(contact)
        }
    }

    fun deleteContact(contact: TrustedContact) {
        viewModelScope.launch {
            contactDao.delete(contact)
        }
    }

    fun setPrimaryContact(contact: TrustedContact) {
        viewModelScope.launch {
            val currentPrimary = _contacts.value.find { it.isPrimary }
            if (currentPrimary != null && currentPrimary.id != contact.id) {
                contactDao.update(currentPrimary.copy(isPrimary = false))
            }
            contactDao.update(contact.copy(isPrimary = true))
        }
    }

    private fun validateContact(contact: TrustedContact): Boolean {
        return contact.name.isNotBlank() && contact.phone.isNotBlank()
    }
}
