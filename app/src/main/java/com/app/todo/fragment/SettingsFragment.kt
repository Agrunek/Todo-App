package com.app.todo.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doOnTextChanged
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.app.todo.R
import com.app.todo.dataStore
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val DONE_KEY = "done"
const val CATEGORY_KEY = "category"
const val DELAY_KEY = "delay"

class SettingsFragment : DialogFragment(R.layout.fragment_settings) {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var done: SwitchMaterial
    private lateinit var category: TextInputLayout
    private lateinit var delay: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = view.findViewById(R.id.settings_app_bar)
        done = view.findViewById(R.id.settings_hide_done_switch)
        category = view.findViewById(R.id.settings_category)
        delay = view.findViewById(R.id.settings_delay)

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        done.isChecked = runBlocking {
            requireContext().dataStore.data.first()[booleanPreferencesKey(DONE_KEY)] ?: false
        }

        done.setOnCheckedChangeListener { _, value ->
            lifecycleScope.launch {
                requireContext().dataStore.edit {
                    it[booleanPreferencesKey(DONE_KEY)] = value
                }
            }
        }

        val items = listOf("", "None", "Home", "Work", "School", "Shopping")
        val adapter = ArrayAdapter(view.context, R.layout.list_item, items)
        val auto = category.editText as? AutoCompleteTextView
        auto?.setAdapter(adapter)
        auto?.setText(runBlocking {
            requireContext().dataStore.data.first()[stringPreferencesKey(
                CATEGORY_KEY
            )] ?: ""
        }, false)

        category.editText?.doOnTextChanged { text, _, _, _ ->
            lifecycleScope.launch {
                requireContext().dataStore.edit {
                    it[stringPreferencesKey(CATEGORY_KEY)] = text?.toString() ?: ""
                }
            }
        }

        delay.value = runBlocking {
            requireContext().dataStore.data.first()[floatPreferencesKey(DELAY_KEY)] ?: 5.0f
        }

        delay.addOnChangeListener { _, value, _ ->
            lifecycleScope.launch {
                requireContext().dataStore.edit {
                    it[floatPreferencesKey(DELAY_KEY)] = value
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
    }
}