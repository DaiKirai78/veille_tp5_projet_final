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
import android.util.Log
import com.example.veille_tp5_projet_final.database.StepDatabase
import com.example.veille_tp5_projet_final.database.StepRecord
import com.example.veille_tp5_projet_final.database.TimerRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private var currentDay: String = ""

    private var isRunning: Boolean = false

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

    private fun sendElapsedTimeUpdate() {
        val elapsedTimeIntent = Intent("com.example.veille_tp5_projet_final.ELAPSED_TIME_UPDATE")
        elapsedTimeIntent.putExtra("elapsedTime", elapsedTime)
        sendBroadcast(elapsedTimeIntent)
        Log.d("StepCounterService", "Elapsed Time Broadcast Sent: $elapsedTime")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true

        val database = StepDatabase.getDatabase(applicationContext)
        val stepDao = database.stepDao()

        CoroutineScope(Dispatchers.IO).launch {
            val timerRecord = stepDao.getTimerForDate(dateFormatter.format(Date()))
            elapsedTime = timerRecord?.timeElapsed ?: 0L
            startTime = System.currentTimeMillis() - elapsedTime

            Log.d("StepCounterService", "Recalculated Start Time: $startTime, Elapsed Time: $elapsedTime")
        }

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        CoroutineScope(Dispatchers.Default).launch {
            while (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                sendElapsedTimeUpdate()

                CoroutineScope(Dispatchers.IO).launch {
                    stepDao.insertOrUpdateTimer(TimerRecord(currentDay, elapsedTime))
                }

                delay(1000L)
            }
        }

        return START_STICKY
    }


    override fun onDestroy() {
        isRunning = false

        val database = StepDatabase.getDatabase(applicationContext)
        val stepDao = database.stepDao()

        elapsedTime = System.currentTimeMillis() - startTime

        CoroutineScope(Dispatchers.IO).launch {
            stepDao.insertOrUpdateTimer(TimerRecord(currentDay, elapsedTime))
            Log.d("StepCounterService", "Saved Elapsed Time: $elapsedTime")
        }

        super.onDestroy()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val today = dateFormatter.format(Date())

            if (currentDay != today) {
                currentDay = today
                stepsToday = 0
                elapsedTime = 0
            }

            val database = StepDatabase.getDatabase(applicationContext)
            val stepDao = database.stepDao()

            CoroutineScope(Dispatchers.IO).launch {
                stepsToday = stepDao.getStepsForDate(today)?.steps ?: 0
                if (!isInitialStepCaptured) {
                    stepsToday++
                } else  {
                    isInitialStepCaptured = false
                }
                if (stepDao.getStepsForDate(today)?.isRunning == true) {
                    stepDao.insertOrUpdateStep(StepRecord(today, stepsToday, true))
                } else {
                    stepDao.insertOrUpdateStep(StepRecord(today, stepsToday, false))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null
}