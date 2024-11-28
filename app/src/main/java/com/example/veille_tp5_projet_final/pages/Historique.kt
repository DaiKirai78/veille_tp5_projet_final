package com.example.veille_tp5_projet_final.pages

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import co.yml.charts.axis.AxisData
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
import co.yml.charts.ui.wavechart.WaveChart
import co.yml.charts.ui.wavechart.model.Wave
import co.yml.charts.ui.wavechart.model.WaveChartData
import co.yml.charts.ui.wavechart.model.WavePlotData
import com.example.veille_tp5_projet_final.database.StepDatabase
import com.example.veille_tp5_projet_final.database.StepRecord
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

@Composable
fun HistoriqueScreen(navController: NavHostController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database by remember { mutableStateOf(StepDatabase.getDatabase(context)) }
    val stepDao = database.stepDao()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var allSteps by remember { mutableStateOf<List<StepRecord>>(emptyList()) }
    var chartData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(today) {
        scope.launch {
            try {
                val allStepsFromDB = stepDao.getAllSteps()
                allSteps = allStepsFromDB

                val allDates = (0..29).map { offset ->
                    Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -offset)
                    }.time.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    }
                }

                chartData = allDates.map { date ->
                    val stepsForDate = allSteps.firstOrNull { it.date == date }?.steps ?: 0
                    date to stepsForDate
                }.reversed()
            } catch (e: Exception) {
                Toast.makeText(context, "Error retrieving data.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        if (chartData.isNotEmpty()) {
            ComposeLineChart(
                data = chartData
            )
        } else {
            Text(text = "Chargement des données...", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ComposeLineChart(
    data: List<Pair<String, Int>>
) {
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
            (i * (maxSteps / 6.0)).toInt().toString()
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(color = Color.White)
    ) {
        item {
            Text(
                text = "Nombre de pas dans les 30 derniers jours",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(color = Color.White),
                lineChartData = theData
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "x : Date\ny : Nombre de pas",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}