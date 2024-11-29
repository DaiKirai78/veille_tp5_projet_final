package com.example.veille_tp5_projet_final.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat.startForegroundService
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.veille_tp5_projet_final.R
import com.example.veille_tp5_projet_final.database.StepDatabase
import com.example.veille_tp5_projet_final.database.StepRecord
import com.example.veille_tp5_projet_final.ui.theme.PaleBlue
import com.example.veille_tp5_projet_final.viewModel.AccueilViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.veille_tp5_projet_final.backgroundProcess.StepCounterService
import com.example.veille_tp5_projet_final.factory.AccueilViewModelFactory

class Accueil : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasPermission = requestActivityRecognitionPermission(this)

        if (hasPermission) {
            startStepCounterService()
        }

        setContent {
            AccueilScreen()
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        startForegroundService(this, intent)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AccueilScreen() {
    val navController = rememberNavController()
    var isPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel: AccueilViewModel = viewModel(
        factory = AccueilViewModelFactory(context)
    )
    val objectif by viewModel.objectif.collectAsState()

    val scope = rememberCoroutineScope()
    val database by remember { mutableStateOf(StepDatabase.getDatabase(context)) }
    val stepDao = database.stepDao()

    var initialStepCount by remember { mutableIntStateOf(0) }
    var isInitialStepCaptured by remember { mutableStateOf(true) }
    var stepsToday by remember { mutableIntStateOf(0) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var isListenerRegistered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isPermissionGranted = requestActivityRecognitionPermission(context)
    }

    DisposableEffect(navController) {
        val callback = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "accueil") {
                scope.launch {
                    viewModel.fetchObjectifForToday(today)
                }
            }
        }
        navController.addOnDestinationChangedListener(callback)
        onDispose {
            navController.removeOnDestinationChangedListener(callback)
        }
    }

    LaunchedEffect(today) {
        if (isPermissionGranted) {
            scope.launch {
                try {
                    val record = stepDao.getStepsForDate(today)
                    stepsToday = record?.steps ?: 0
                    initialStepCount = 0
                    viewModel.fetchObjectifForToday(today)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error retrieving data.", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted && !isListenerRegistered) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            val stepListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                        scope.launch {
                            stepsToday = stepDao.getStepsForDate(today)?.steps ?: 0
                            if(!isInitialStepCaptured) {
                                stepsToday++
                            } else  {
                                isInitialStepCaptured = false
                            }
                            println("currentSteps : $stepsToday")
                            stepDao.insertOrUpdateStep(StepRecord(today, stepsToday))
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            stepSensor?.let { sensor ->
                sensorManager.registerListener(stepListener, sensor, SensorManager.SENSOR_DELAY_UI)
                isListenerRegistered = true
            } ?: run {
                Toast.makeText(context, "Capteur de podomètre non disponible", Toast.LENGTH_LONG).show()
            }
        }
    }

    NavHost(navController = navController, startDestination = "accueil") {
        composable("accueil") {
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
                    Button(onClick = {navController.navigate("historique")}, colors = ButtonDefaults.buttonColors(PaleBlue), contentPadding = PaddingValues(start = 38.dp, end = 38.dp, top = 10.dp, bottom = 10.dp), modifier = Modifier.align(Alignment.TopCenter).padding(top = 150.dp)) {
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
                            currentValue = stepsToday,
                            targetValue = objectif,
                            progressBarColor = PaleBlue
                        )
                        Spacer(modifier = Modifier.height(30.dp))
                        Box(modifier = Modifier.fillMaxSize()) {
                            Button(
                                onClick = {
                                    navController.navigate("parametre") },
                                colors = ButtonDefaults.buttonColors(PaleBlue),
                                contentPadding = PaddingValues(
                                    start = 38.dp,
                                    end = 38.dp,
                                    top = 10.dp,
                                    bottom = 10.dp
                                ),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
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
        composable("parametre") { ParametreScreen(navController) }
        composable("historique") { HistoriqueScreen(navController) }
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
