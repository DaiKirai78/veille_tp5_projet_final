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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.veille_tp5_projet_final.R
import com.example.veille_tp5_projet_final.ui.theme.PaleBlue

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
    var stepCount by remember { mutableIntStateOf(0) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val number by remember { mutableIntStateOf(6000) }

    var initialStepCount by remember { mutableIntStateOf(0) }
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
        modifier = Modifier.fillMaxSize()
    ) {
        if (isPermissionGranted) {
            Text(
                text = "Bougez, Respirez, Vivez pleinement.",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 48.dp, end = 32.dp, bottom = 16.dp),
                fontFamily = FontFamily(Font(R.font.lexend_mega_variable_font_wght, FontWeight.Bold)),
            )
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(PaleBlue), contentPadding = PaddingValues(start = 38.dp, end = 38.dp, top = 10.dp, bottom = 10.dp), modifier = Modifier.align(Alignment.TopCenter).padding(top = 150.dp)) {
                Text("Historique", color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 225.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPermissionGranted) {
                CircularProgressBar(
                    currentValue = stepCount,
                    targetValue = number,
                    progressBarColor = PaleBlue
                )
                Spacer(modifier = Modifier.height(30.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(PaleBlue),
                        contentPadding = PaddingValues(
                            start = 38.dp,
                            end = 38.dp,
                            top = 10.dp,
                            bottom = 10.dp
                        ),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Icons.Default.Settings
                        Text("Paramètre", color = Color.White)
                    }
                }
            } else {
                Text(
                    text = "Permission non accordée ou capteur non disponible. Veuillez accorder la permission d'activité et redémarrer l'application.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun CircularProgressBar(
    currentValue: Int,
    targetValue: Int,
    modifier: Modifier = Modifier,
    progressBarColor: Color = PaleBlue,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    strokeWidth: Float = 30f,
) {
    val progress = if (targetValue > 0) currentValue.toFloat() / targetValue else 0f
    val percentage = (progress * 100).coerceIn(0f, 100f).toInt()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(300.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = backgroundColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = progressBarColor,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$currentValue / $targetValue",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.baseline_directions_walk_24),
                contentDescription = null,
                tint = PaleBlue,
                modifier = Modifier.size(60.dp)
            )
        }
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
