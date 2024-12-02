package com.example.veille_tp5_projet_final.pages

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.roundTwoDecimal
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.veille_tp5_projet_final.database.StepDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Historique : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) {
        paddingValues -> Box(modifier = Modifier.padding(paddingValues)) {
            HistoriqueGraphique()
        }
    }
}

@Composable
fun HistoriqueGraphique() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database by remember { mutableStateOf(StepDatabase.getDatabase(context)) }
    val stepDao = database.stepDao()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var stepChartData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var timeChartData by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }

    LaunchedEffect(today) {
        scope.launch {
            val allSteps = stepDao.getAllSteps()
            val allTimers = stepDao.getAllTimers()

            val allDates = (0..29).map { offset ->
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                }.time.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                }
            }

            stepChartData = allDates.map { date ->
                val stepsForDate = allSteps.firstOrNull { it.date == date }?.steps ?: 0
                date to stepsForDate
            }.reversed()

            timeChartData = allDates.map { date ->
                val timeForDate = allTimers.firstOrNull { it.date == date }?.timeElapsed ?: 0L
                date to timeForDate
            }.reversed()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Nombre de pas dans les 30 derniers jours",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            if (stepChartData.isNotEmpty()) {
                ComposeLineChart(data = stepChartData)
            } else {
                Text(text = "Chargement des données des pas...")
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Temps passé dans les 30 derniers jours (en format 0h0m0s)",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            if (timeChartData.isNotEmpty()) {
                ComposeLineChartForTime(data = timeChartData)
            } else {
                Text(text = "Chargement des données de temps...")
            }
        }
    }

}

@Composable
fun ComposeLineChart(data: List<Pair<String, Int>>) {
    val pointsData = data.mapIndexed { index, map ->
        Point(index.toFloat(), map.second.toFloat())
    }

    val xAxisLabels = data.map { date ->
        val dateFormat = SimpleDateFormat("dd MMM", Locale.FRENCH)
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date.first)
        dateFormat.format(formattedDate!!)
    }

    val xAxisData = AxisData.Builder()
        .steps(data.size - 1)
        .axisStepSize(80.dp)
        .backgroundColor(Color.White)
        .labelData { i -> xAxisLabels.getOrElse(i) { "" } }
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(6)
        .backgroundColor(Color.White)
        .labelData { i ->
            val maxSteps = data.maxOf { it.second }
            val stepValue = (i * (maxSteps / 6.0))
            formatLargeNumber(stepValue)
        }
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .labelAndAxisLinePadding(10.dp)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .build()

    LineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        lineChartData = LineChartData(
            linePlotData = LinePlotData(
                lines = listOf(
                    Line(
                        dataPoints = pointsData,
                        lineStyle = LineStyle(
                            lineType = LineType.SmoothCurve(),
                            color = MaterialTheme.colorScheme.tertiary,
                        ),
                        shadowUnderLine = ShadowUnderLine(
                            alpha = 0.5f,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.inversePrimary,
                                    Color.Transparent
                                )
                            ),
                            color = Color.Gray.copy(alpha = 0.3f),
                        ),
                        intersectionPoint = IntersectionPoint(
                            color = MaterialTheme.colorScheme.tertiary,
                            radius = 3.dp
                        ),
                        selectionHighlightPoint = SelectionHighlightPoint(
                            color = MaterialTheme.colorScheme.tertiary,
                            radius = 4.dp
                        ),
                        selectionHighlightPopUp = SelectionHighlightPopUp(
                            backgroundColor = Color.White,
                            labelColor = Color.Black,
                            labelTypeface = Typeface.DEFAULT_BOLD,
                        )
                    )
                )
            ),
            backgroundColor = Color.White,
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            gridLines = GridLines(color = MaterialTheme.colorScheme.outlineVariant)
        )
    )
}

@Composable
fun ComposeLineChartForTime(data: List<Pair<String, Long>>) {
    val pointsData = data.mapIndexed { index, map ->
        Point(index.toFloat(), map.second/1000.toFloat())
    }

    val xAxisLabels = data.map { date ->
        val dateFormat = SimpleDateFormat("dd MMM", Locale.FRENCH)
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date.first)
        dateFormat.format(formattedDate!!)
    }

    val xAxisData = AxisData.Builder()
        .steps(data.size - 1)
        .axisStepSize(80.dp)
        .backgroundColor(Color.White)
        .labelData { i -> xAxisLabels.getOrElse(i) { "" } }
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(6)
        .backgroundColor(Color.White)
        .labelData { i ->
            val maxSeconds = data.maxOf { it.second }
            val stepSeconds = ((i * maxSeconds) / 6.0).toLong()
            formatTime(stepSeconds)
        }
        .axisLineColor(MaterialTheme.colorScheme.tertiary)
        .axisLabelColor(MaterialTheme.colorScheme.tertiary)
        .build()

    val theData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    lineStyle = LineStyle(
                        lineType = LineType.SmoothCurve(),
                        color = MaterialTheme.colorScheme.tertiary,
                    ),
                    shadowUnderLine = ShadowUnderLine(
                        alpha = 0.5f,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.inversePrimary,
                                Color.Transparent
                            )
                        ),
                        color = Color.Gray.copy(alpha = 0.3f),
                    ),
                    intersectionPoint = IntersectionPoint(
                        color = MaterialTheme.colorScheme.tertiary,
                        radius = 3.dp
                    ),
                    selectionHighlightPoint = SelectionHighlightPoint(
                        color = MaterialTheme.colorScheme.tertiary,
                        radius = 4.dp
                    ),
                    selectionHighlightPopUp = SelectionHighlightPopUp(
                        backgroundColor = Color.White,
                        labelColor = Color.Black,
                        labelTypeface = Typeface.DEFAULT_BOLD,
                    )
                )
            )
        ),
        backgroundColor = Color.White,
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(color = MaterialTheme.colorScheme.outlineVariant),
    )

    LineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        lineChartData = theData
    )
}


fun formatTime(seconds: Long): String {
    val lesSecondes = seconds / 1000
    val hours = lesSecondes / 3600
    if (lesSecondes < 3600) {
        val minutes = lesSecondes / 60
        val secs = lesSecondes % 60
        if (minutes < 10 && secs < 10) {
            return "0${minutes}m0${secs}s"
        }
        if (minutes < 10) {
            return "0${minutes}m${secs}s"
        }
        if (secs < 10) {
            return "${minutes}m0${secs}s"
        }
        return "${minutes}m${secs}s"
    }
    val minutes = (lesSecondes % 3600) / 60
    val secs = lesSecondes % 60
    if (minutes < 10 && secs < 10) {
        return "${hours}h0${minutes}m0${secs}s"
    }
    if (minutes < 10) {
        return "${hours}h0${minutes}m${secs}s"
    }
    if (secs < 10) {
        return "${hours}h${minutes}m0${secs}s"
    }
    return "${hours}h${minutes}m${secs}s"
}

fun formatLargeNumber(value: Double): String {
    return when {
        value >= 1_000_000 -> "${(value / 1_000_000).roundTwoDecimal()}M"
        value >= 1_000 -> "${(value / 1_000).roundTwoDecimal()}K"
        else -> value.toInt().toString()
    }
}