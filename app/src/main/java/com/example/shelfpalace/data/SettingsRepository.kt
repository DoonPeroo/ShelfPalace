package com.example.shelfpalace.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val disabledIdsKey = stringSetPreferencesKey("disabled_ids")
    private val sortOptionKey = stringPreferencesKey("sort_option")
    private val dashboardFilterKey = stringPreferencesKey("dashboard_filter")
    private val cornerStyleKey = stringPreferencesKey("corner_style")

    val disabledIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[disabledIdsKey] ?: emptySet()
    }

    val cornerStyle: Flow<CornerStyle> = context.dataStore.data.map { preferences ->
        val name = preferences[cornerStyleKey] ?: CornerStyle.ROUNDED.name
        try {
            CornerStyle.valueOf(name)
        } catch (e: Exception) {
            CornerStyle.ROUNDED
        }
    }

    suspend fun setCornerStyle(style: CornerStyle) {
        context.dataStore.edit { preferences ->
            preferences[cornerStyleKey] = style.name
        }
    }

    val sortOption: Flow<SortOption> = context.dataStore.data.map { preferences ->
        val name = preferences[sortOptionKey] ?: SortOption.NAME.name
        try {
            SortOption.valueOf(name)
        } catch (e: Exception) {
            SortOption.NAME
        }
    }

    suspend fun toggleVisibility(id: String, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[disabledIdsKey] ?: emptySet()
            val updated = if (isEnabled) {
                current - id
            } else {
                current + id
            }
            preferences[disabledIdsKey] = updated
        }
    }

    suspend fun setSortOption(option: SortOption) {
        context.dataStore.edit { preferences ->
            preferences[sortOptionKey] = option.name
        }
    }

    val dashboardFilter: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[dashboardFilterKey] ?: "Recently Added"
    }

    suspend fun setDashboardFilter(filter: String) {
        context.dataStore.edit { preferences ->
            preferences[dashboardFilterKey] = filter
        }
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun restoreSettings(
        disabledIds: Set<String>,
        sortOption: SortOption,
        dashboardFilter: String,
        cornerStyle: CornerStyle
    ) {
        context.dataStore.edit { preferences ->
            preferences[disabledIdsKey] = disabledIds
            preferences[sortOptionKey] = sortOption.name
            preferences[dashboardFilterKey] = dashboardFilter
            preferences[cornerStyleKey] = cornerStyle.name
        }
    }
}
