package com.example.myfit.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.prefs.SecurePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = MyFitApp.from(application).securePrefs
    private val profileDao = MyFitApp.from(application).database.userProfileDao()

    val profile = profileDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var selectedModel by mutableStateOf(prefs.apiModel)
        private set

    val models = SecurePrefs.AVAILABLE_MODELS

    fun selectModel(model: String) {
        prefs.apiModel = model
        selectedModel = model
    }

    // ── Edit profile ──────────────────────────────────────────
    var showEditProfile by mutableStateOf(false)
        private set
    var editAge by mutableStateOf("")
    var editGender by mutableStateOf("male")
    var editHeight by mutableStateOf("")
    var editWeight by mutableStateOf("")
    var editGoal by mutableStateOf("maintain")
    var editActivity by mutableStateOf(1.55f)
    var editError by mutableStateOf<String?>(null)

    fun openEditProfile() {
        val p = profile.value ?: return
        editAge = p.age.toString()
        editGender = p.gender
        editHeight = p.height_cm.toInt().toString()
        editWeight = "%.1f".format(p.weight_kg)
        editGoal = p.goal
        editActivity = p.activity_level
        editError = null
        showEditProfile = true
    }

    fun dismissEditProfile() { showEditProfile = false }

    fun saveProfile() {
        val age = editAge.toIntOrNull()
        val height = editHeight.replace(",", ".").toFloatOrNull()
        val weight = editWeight.replace(",", ".").toFloatOrNull()

        when {
            age == null || age !in 10..120 -> { editError = "Возраст: 10–120 лет"; return }
            height == null || height !in 100f..250f -> { editError = "Рост: 100–250 см"; return }
            weight == null || weight !in 20f..300f -> { editError = "Вес: 20–300 кг"; return }
        }

        viewModelScope.launch {
            val current = profile.value
            profileDao.upsert(UserProfile(
                id = 1,
                age = age!!,
                gender = editGender,
                height_cm = height!!,
                weight_kg = weight!!,
                current_weight_kg = current?.current_weight_kg ?: weight!!,
                goal = editGoal,
                activity_level = editActivity,
                api_key_set = current?.api_key_set ?: false,
                created_at = current?.created_at ?: System.currentTimeMillis()
            ))
            showEditProfile = false
        }
    }
}
