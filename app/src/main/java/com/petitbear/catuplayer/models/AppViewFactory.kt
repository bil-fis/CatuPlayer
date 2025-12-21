// models/AppViewModelFactory.kt
package com.petitbear.catuplayer.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.petitbear.catuplayer.data.AppDatabase
import com.petitbear.catuplayer.data.SongRepository

class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioPlayerViewModel::class.java)) {
            val database = AppDatabase.getInstance(application)
            val songRepository = SongRepository(database.songDao())
            return AudioPlayerViewModel(application, songRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}