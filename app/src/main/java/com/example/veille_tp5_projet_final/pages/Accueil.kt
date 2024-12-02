package com.example.veille_tp5_projet_final.pages

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.animation.animateColorAsState
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
import com.example.veille_tp5_projet_final.database.TimerRecord
import com.example.veille_tp5_projet_final.factory.AccueilViewModelFactory
import kotlinx.coroutines.delay

class Accueil : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AccueilScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AccueilScreen() {
    val navController = rememberNavController()
    var isPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AccueilViewModel = viewModel(
        factory = AccueilViewModelFactory(application)
    )
    val objectif by viewModel.objectif.collectAsState()

    val scope = rememberCoroutineScope()
    val database by remember { mutableStateOf(StepDatabase.getDatabase(context)) }
    val stepDao = database.stepDao()

    var initialStepCount by remember { mutableIntStateOf(0) }
    var stepsToday by remember { mutableIntStateOf(0) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableLongStateOf(0L) }

    val isObjectiveReached = stepsToday >= objectif
    val backgroundColor = if (isObjectiveReached) Color(0x80CCFFCC) else Color(0x80CCFFFF)

    val boutonColor = if (isObjectiveReached) Color(0x80339900) else Color(0xCC3399FF)

    val progressBarColor by animateColorAsState(
        targetValue = if (isObjectiveReached) Color(0xFF43A047) else Color(0xFF1E88E5)
    )

    var isListenerRegistered by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (stepDao.getStepsForDate(today)?.isRunning == true) {
            while (isRunning) {
                elapsedTime += 1000
                delay(1000L)
            }
        }
    }

    DisposableEffect(Unit) {
        val elapsedTimeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                elapsedTime = intent?.getLongExtra("elapsedTime", 0L) ?: 0L
            }
        }
        val intentFilter = IntentFilter("com.example.veille_tp5_projet_final.ELAPSED_TIME_UPDATE")
        context.registerReceiver(elapsedTimeReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        isPermissionGranted = requestPermissions(context)

        onDispose {
            context.unregisterReceiver(elapsedTimeReceiver)
        }
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
                    val recordTimer = stepDao.getTimerForDate(today)
                    elapsedTime = recordTimer?.timeElapsed ?: 0L
                    val record = stepDao.getStepsForDate(today)
                    stepsToday = record?.steps ?: 0
                    isRunning = record?.isRunning == true
                    initialStepCount = 0
                    viewModel.fetchObjectifForToday(today)
                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur de récupération des données.", Toast.LENGTH_LONG).show()
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
            Scaffold(
                containerColor = backgroundColor,
                topBar = {
                    Text(
                        text = if (isObjectiveReached) "Félicitations, objectif atteint !" else "Bougez, Respirez, Vivez pleinement.",
                        fontFamily = FontFamily(Font(R.font.lexend_mega_variable_font_wght)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 40.dp, end = 16.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isPermissionGranted) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formatElapsedTime(elapsedTime),
                                fontSize = 26.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            CircularProgressBar(
                                currentValue = stepsToday,
                                targetValue = objectif,
                                progressBarColor = progressBarColor,
                                backgroundColor = Color(0xFFE3F2FD)
                            )
                            Spacer(modifier = Modifier.height(26.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val currentRecord = stepDao.getStepsForDate(today)
                                        val isCurrentlyRunning = currentRecord?.isRunning == true
                                        if (!isCurrentlyRunning) {
                                            stepDao.insertOrUpdateStep(
                                                StepRecord(today, currentRecord?.steps ?: 0, true)
                                            )
                                            isRunning = true
                                            val intent = Intent(context, StepCounterService::class.java)
                                            context.startForegroundService(intent)
                                        } else {
                                            stepDao.insertOrUpdateStep(
                                                StepRecord(today, currentRecord?.steps ?: 0, false)
                                            )
                                            val intent = Intent(context, StepCounterService::class.java)
                                            context.stopService(intent)
                                            stepDao.insertOrUpdateTimer(
                                                TimerRecord(today, elapsedTime)
                                            )
                                            isRunning = false
                                        }
                                    }
                                },
                                colors = if (isRunning) ButtonDefaults.buttonColors(Color(0x80FF0000)) else ButtonDefaults.buttonColors(boutonColor),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text(text = if (isRunning) "STOP" else "START", color = Color.White, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(26.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Button(
                                    onClick = { navController.navigate("historique") },
                                    colors = ButtonDefaults.buttonColors(boutonColor),
                                ) {
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Text("Historique", color = Color.White)
                                    Spacer(modifier = Modifier.width(20.dp))
                                }
                                Button(
                                    onClick = { navController.navigate("parametre") },
                                    colors = ButtonDefaults.buttonColors(boutonColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                                    Text("Paramètre", color = Color.White)
                                }
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
                tint = progressBarColor,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun requestPermissions(context: Context): Boolean {
    val requiredPermissions = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        requiredPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return if (requiredPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            context as Activity,
            requiredPermissions.toTypedArray(),
            1
        )
        false
    } else {
        true
    }
}


fun formatElapsedTime(elapsedTime: Long): String {
    val seconds = (elapsedTime / 1000) % 60
    val minutes = (elapsedTime / (1000 * 60)) % 60
    val hours = (elapsedTime / (1000 * 60 * 60))
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
