package com.app.todo

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.todo.db.AppDatabase
import com.app.todo.db.Task
import com.app.todo.db.TaskDao
import com.app.todo.fragment.CATEGORY_KEY
import com.app.todo.fragment.DONE_KEY
import com.app.todo.fragment.SettingsFragment
import com.app.todo.fragment.TaskEditFragment
import com.app.todo.util.Notificator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity(), DialogInterface.OnDismissListener {

    private lateinit var taskDao: TaskDao
    private lateinit var noti: Notificator

    private lateinit var toolbar: MaterialToolbar
    private lateinit var search: TextInputLayout
    private lateinit var scrollview: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        taskDao = AppDatabase.instance(this).taskDao()
        noti = Notificator(this)

        scrollview = findViewById(R.id.main_scrollview)

        scrollview.layoutManager = LinearLayoutManager(this)

        val taskAddButton: FloatingActionButton = findViewById(R.id.main_task_add_button)

        taskAddButton.setOnClickListener {
            TaskEditFragment().show(supportFragmentManager, "TaskEditFragment")
        }

        scrollview.adapter = MainAdapter(taskDao.getAll().sortedBy { it.expiration }.filter {
            val done = runBlocking {
                dataStore.data.first()[booleanPreferencesKey(DONE_KEY)] ?: false
            }

            !it.finished || !done
        }.filter {
            val category = runBlocking {
                dataStore.data.first()[stringPreferencesKey(CATEGORY_KEY)] ?: ""
            }

            category.isEmpty() || it.category.contains(category, true)
        })

        search = findViewById(R.id.main_search)

        search.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                scrollview.adapter = MainAdapter(taskDao.getAll().sortedBy { it.expiration }
                    .filter { it.title.contains(text, true) }.filter {
                        val done = runBlocking {
                            dataStore.data.first()[booleanPreferencesKey(DONE_KEY)] ?: false
                        }

                        !it.finished || !done
                    }.filter {
                        val category = runBlocking {
                            dataStore.data.first()[stringPreferencesKey(CATEGORY_KEY)] ?: ""
                        }

                        category.isEmpty() || it.category.contains(category, true)
                    })
            } else {
                scrollview.adapter =
                    MainAdapter(taskDao.getAll().sortedBy { it.expiration }.filter {
                        val done = runBlocking {
                            dataStore.data.first()[booleanPreferencesKey(DONE_KEY)] ?: false
                        }

                        !it.finished || !done
                    }.filter {
                        val category = runBlocking {
                            dataStore.data.first()[stringPreferencesKey(CATEGORY_KEY)] ?: ""
                        }

                        category.isEmpty() || it.category.contains(category, true)
                    })
            }
        }

        toolbar = findViewById(R.id.main_app_bar)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> {
                    SettingsFragment().show(supportFragmentManager, "SettingsFragment")
                    true
                }

                else -> false
            }
        }
    }

    private inner class MainAdapter(private val tasks: List<Task>) :
        RecyclerView.Adapter<MainAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.task_card, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = tasks.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(tasks[position])
        }

        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val card: MaterialCardView
            private val name: TextView
            private val done: CheckBox
            private val description: TextView
            private val category: TextView
            private val expiration: TextView

            init {
                card = view.findViewById(R.id.task_card)
                name = view.findViewById(R.id.task_card_name)
                done = view.findViewById(R.id.task_card_done)
                description = view.findViewById(R.id.task_card_description)
                category = view.findViewById(R.id.task_card_category)
                expiration = view.findViewById(R.id.task_card_expiration)
            }

            fun bind(item: Task) {
                name.text = item.title
                done.isChecked = item.finished
                description.text = item.description
                category.text = item.category
                expiration.text = if (item.expiration > Instant.now()
                        .toEpochMilli()
                ) SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(item.expiration)) else "expired"
                card.setOnClickListener {
                    TaskEditFragment(item).show(supportFragmentManager, "TaskEditFragment")
                }
                done.setOnCheckedChangeListener { _, value ->

                    val privateTask = Task(
                        id = item.id,
                        title = item.title,
                        description = item.description,
                        creation = item.creation,
                        expiration = item.expiration,
                        finished = value,
                        notify = item.notify,
                        category = item.category,
                        attachment = item.attachment
                    )

                    taskDao.updateTasks(privateTask)
                }
            }
        }
    }

    override fun onDismiss(dialogInterface: DialogInterface?) {
        scrollview.adapter = MainAdapter(taskDao.getAll().sortedBy { it.expiration }.filter {
            val done = runBlocking {
                dataStore.data.first()[booleanPreferencesKey(DONE_KEY)] ?: false
            }

            !it.finished || !done
        }.filter {
            val category = runBlocking {
                dataStore.data.first()[stringPreferencesKey(CATEGORY_KEY)] ?: ""
            }

            category.isEmpty() || it.category.contains(category, true)
        })
    }

    override fun onStop() {
        super.onStop()
        noti.scheduleNotiQ(taskDao.getAll())
    }
}