package com.example.veille_tp5_projet_final.backgroundProcess

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.veille_tp5_projet_final.database.StepDatabase
import com.example.veille_tp5_projet_final.database.StepRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepCounterService : android.app.Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isInitialStepCaptured = false
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var stepsToday = 0

    private var startTime: Long = 0
    private var elapsedTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val channelId = "StepCounterChannel"
        val channel = NotificationChannel(
            channelId,
            "Step Counter Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Comptage des pas")
            .setContentText("Le service est en cours d'exécution")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        startTime = System.currentTimeMillis() - elapsedTime
        return START_STICKY
    }

    override fun onDestroy() {
        elapsedTime = System.currentTimeMillis() - startTime
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    fun getElapsedTime(): Long {
        return System.currentTimeMillis() - startTime
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val today = dateFormatter.format(Date())
            val database = StepDatabase.getDatabase(applicationContext)
            val stepDao = database.stepDao()

            CoroutineScope(Dispatchers.IO).launch {
                stepsToday = stepDao.getStepsForDate(today)?.steps ?: 0
                if (!isInitialStepCaptured) {
                    stepsToday++
                } else  {
                    isInitialStepCaptured = false
                }
                stepDao.insertOrUpdateStep(StepRecord(today, stepsToday))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null
}