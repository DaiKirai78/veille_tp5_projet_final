package com.example.veille_tp5_projet_final.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Acceuil : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AcceuilScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AcceuilScreen() {
    var stepCount by remember { mutableStateOf(0) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var initialStepCount by remember { mutableStateOf(0) }
    var isInitialStepCaptured by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val permissionStatus = requestActivityRecognitionPermission(context)
        isPermissionGranted = permissionStatus
    }

    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            stepSensor?.let { sensor ->
                sensorManager.registerListener(object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                            val currentStepCount = event.values[0].toInt()

                            if (!isInitialStepCaptured) {
                                initialStepCount = currentStepCount
                                isInitialStepCaptured = true
                            }

                            stepCount = currentStepCount - initialStepCount
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }, sensor, SensorManager.SENSOR_DELAY_UI)
            } ?: run {
                Toast.makeText(context, "Capteur de podomètre non disponible", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPermissionGranted) {
                "Nombre de pas : $stepCount"
            } else {
                "Permission non accordée ou capteur non disponible."
            },
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 25.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun requestActivityRecognitionPermission(context: Context): Boolean {
    return if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
        true
    } else {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            1
        )
        false
    }
}
