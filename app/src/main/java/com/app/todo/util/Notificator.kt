package com.app.todo.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.app.todo.MainActivity
import com.app.todo.R
import com.app.todo.dataStore
import com.app.todo.db.Task
import com.app.todo.fragment.DELAY_KEY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "APP"
private const val CHANNEL_NAME = "TODO"
private const val CHANNEL_DESCRIPTION = "null"
private const val NOTIFICATION_ID = 1

class Notificator(val context: Context) {

    private fun createNotiChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNoti(title: String, message: String) {

        createNotiChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon =
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_edit_notifications_24)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_edit_notifications_24).setLargeIcon(icon)
            .setContentTitle(title).setContentText(message).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(icon)
            ).setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true).build()

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    fun scheduleNotiQ(tasks: List<Task>) {

        val offset = runBlocking {
            context.dataStore.data.first()[floatPreferencesKey(DELAY_KEY)]?.toInt() ?: 5
        }

        clearNotiQ()

        val now = Instant.now().toEpochMilli()

        Log.d("TASK", offset.toString())
        Log.d("TASK", (tasks.first().expiration > now).toString())
        Log.d("TASK", tasks.first().notify.toString())

        tasks.filter { it.expiration > now && it.notify }.sortedBy { it.expiration }.forEach {

            val delay = it.expiration - offset * 60000

            Log.d("TASK", delay.toString())

            val input = Data.Builder().putString("title", it.title)
                .putString("message", "Expires in $offset minutes!").build()

            val work = OneTimeWorkRequestBuilder<NotiWorker>().setInputData(input)
                .setInitialDelay(Duration.between(Instant.ofEpochMilli(now), Instant.ofEpochMilli(delay)).toMillis(), TimeUnit.MILLISECONDS).build()

            WorkManager.getInstance(context).enqueue(work)
            Log.d("TASK", "${it.title} scheduled in ${Duration.between(Instant.ofEpochMilli(now), Instant.ofEpochMilli(delay)).toMinutes()} minutes!")
        }
    }

    private fun clearNotiQ() {
        WorkManager.getInstance(context).cancelAllWork()
        Log.d("TASK", "Cleared whole q!")
    }

    class NotiWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            Notificator(context).createNoti(
                inputData.getString("title").toString(), inputData.getString("message").toString()
            )
            return Result.success()
        }
    }
}