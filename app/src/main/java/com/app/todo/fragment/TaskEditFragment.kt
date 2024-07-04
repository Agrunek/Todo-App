package com.app.todo.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.app.todo.R
import com.app.todo.db.AppDatabase
import com.app.todo.db.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

class TaskEditFragment(private val task: Task? = null) :
    DialogFragment(R.layout.fragment_task_edit) {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var name: TextInputLayout
    private lateinit var description: TextInputLayout
    private lateinit var category: TextInputLayout
    private lateinit var deadline: MaterialTextView
    private lateinit var date: MaterialButton
    private lateinit var time: MaterialButton
    private lateinit var switch: SwitchMaterial
    private lateinit var confirm: MaterialButton

    private var now = Instant.now().toEpochMilli()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = view.findViewById(R.id.edit_app_bar)
        name = view.findViewById(R.id.edit_name)
        description = view.findViewById(R.id.edit_description)
        category = view.findViewById(R.id.edit_category)
        deadline = view.findViewById(R.id.edit_datetime_display)
        date = view.findViewById(R.id.edit_date_picker)
        time = view.findViewById(R.id.edit_time_picker)
        switch = view.findViewById(R.id.edit_switch)
        confirm = view.findViewById(R.id.edit_confirm)

        val items = listOf("None", "Home", "Work", "School", "Shopping")
        val adapter = ArrayAdapter(view.context, R.layout.list_item, items)
        val auto = category.editText as? AutoCompleteTextView
        auto?.setAdapter(adapter)
        auto?.setText("None", false)

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        if (task == null) {
            toolbar.menu.clear()
        } else {
            toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.delete -> {
                        AppDatabase.instance(view.context).taskDao().delete(task)
                        dismiss()
                        true
                    }

                    else -> false
                }
            }
        }



        date.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date")
                .setSelection(now).build()

            picker.addOnPositiveButtonClickListener {
                now = it
                updateDeadline(now)
            }

            picker.show(childFragmentManager, "date")
        }

        time.setOnClickListener {
            val picker = MaterialTimePicker.Builder().setTitleText("Select time").build()

            picker.addOnPositiveButtonClickListener {
                val date = Date(now)
                val format = SimpleDateFormat("dd/MM/yyyy")
                val midnight = format.parse(format.format(date))
                now = midnight?.time ?: 0
                now += (picker.hour * 60 * 60 + picker.minute * 60) * 1000L
                updateDeadline(now)
            }

            picker.show(childFragmentManager, "time")
        }

        confirm.setOnClickListener {

            if (task == null) {

                val privateTask = Task(
                    title = name.editText?.text.toString(),
                    description = description.editText?.text.toString(),
                    creation = Instant.now().toEpochMilli(),
                    expiration = now,
                    finished = false,
                    notify = switch.isChecked,
                    category = category.editText?.text.toString()
                )

                lifecycleScope.launch {
                    AppDatabase.instance(view.context).taskDao().insertAll(privateTask)
                    dismiss()
                }

            } else {

                val privateTask = Task(
                    id = task.id,
                    title = name.editText?.text.toString(),
                    description = description.editText?.text.toString(),
                    creation = Instant.now().toEpochMilli(),
                    expiration = now,
                    finished = false,
                    notify = switch.isChecked,
                    category = category.editText?.text.toString(),
                    attachment = task.attachment
                )

                lifecycleScope.launch {
                    AppDatabase.instance(view.context).taskDao().updateTasks(privateTask)
                    dismiss()
                }
            }
        }

        updateDeadline(now)

        task?.let {
            name.editText?.text = Editable.Factory.getInstance().newEditable(it.title)
            description.editText?.text = Editable.Factory.getInstance().newEditable(it.description)
            now = it.expiration
            auto?.setText(it.category, false)
            updateDeadline(it.expiration)
            switch.isChecked = it.notify
        }
    }

    private fun updateDeadline(newValue: Long) {
        val date = Date(newValue)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm")
        deadline.text = format.format(date)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
    }
}