package com.example.bakalariapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bakalariapp.data.model.AbsencePerSubject
import com.example.bakalariapp.data.model.Mark
import com.example.bakalariapp.data.model.Subject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksDetailScreen(
    subject: Subject,
    absencesPerSubject: List<AbsencePerSubject>,
    onBackClick: () -> Unit
) {
    // Calculate current average
    val currentAverage = remember(subject) {
        val apiAverage = subject.averageText?.trim()?.replace(",", ".")?.toDoubleOrNull()
        apiAverage ?: calculateSubjectAverage(subject.marks)
    }
    val averageFormatted = currentAverage?.let { String.format("%.2f", it) } ?: "-"
    
    // Find absence for this subject - try multiple matching strategies
    val subjectAbsence = remember(subject, absencesPerSubject) {
        val subjectName = subject.subjectInfo.name.trim()
        val subjectAbbrev = subject.subjectInfo.abbrev?.trim() ?: ""
        
        absencesPerSubject.find { absence ->
            val absenceName = absence.subjectName.trim()
            // Exact match
            absenceName.equals(subjectName, ignoreCase = true) ||
            absenceName.equals(subjectAbbrev, ignoreCase = true) ||
            // Contains match (e.g., "Matematika" matches "Matematika - Algebra")
            absenceName.contains(subjectName, ignoreCase = true) ||
            subjectName.contains(absenceName, ignoreCase = true) ||
            // Abbreviation contains
            (subjectAbbrev.isNotEmpty() && absenceName.contains(subjectAbbrev, ignoreCase = true))
        }
    }
    
    // Theoretical mark calculator state
    var theoreticalMark by remember { mutableStateOf("") }
    var theoreticalWeight by remember { mutableIntStateOf(1) }
    
    // Calculate theoretical average
    val theoreticalAverage = remember(currentAverage, theoreticalMark, theoreticalWeight) {
        val markValue = parseMarkValueForCalculator(theoreticalMark)
        if (markValue != null && currentAverage != null) {
            calculateTheoreticalAverage(subject.marks, markValue, theoreticalWeight)
        } else {
            null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = subject.subjectInfo.name.trim(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prominent Average Card at top
            item {
                AverageCard(
                    average = currentAverage,
                    averageFormatted = averageFormatted,
                    markCount = subject.marks.size,
                    absencePercentage = subjectAbsence?.getAbsencePercentage()
                )
            }
            
            // Marks list
            items(
                items = subject.marks.sortedByDescending { it.markDate },
                key = { it.id }
            ) { mark ->
                MarkCard(mark = mark)
            }
            
            // Theoretical Mark Calculator at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TheoreticalMarkCalculator(
                    theoreticalMark = theoreticalMark,
                    onMarkChange = { theoreticalMark = it },
                    theoreticalWeight = theoreticalWeight,
                    onWeightChange = { theoreticalWeight = it },
                    theoreticalAverage = theoreticalAverage,
                    currentAverage = currentAverage
                )
            }
            
            // DEBUG INFO PANEL
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DebugInfoPanel(
                    subject = subject,
                    absencesPerSubject = absencesPerSubject,
                    subjectAbsence = subjectAbsence,
                    apiAverage = subject.averageText,
                    calculatedAverage = currentAverage,
                    numericMarksCount = subject.marks.filter { !it.isPoints }.size,
                    totalMarksCount = subject.marks.size
                )
            }
        }
    }
}

@Composable
fun DebugInfoPanel(
    subject: Subject,
    absencesPerSubject: List<AbsencePerSubject>,
    subjectAbsence: AbsencePerSubject?,
    apiAverage: String?,
    calculatedAverage: Double?,
    numericMarksCount: Int,
    totalMarksCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "DEBUG INFO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Subject: ${subject.subjectInfo.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Abbrev: ${subject.subjectInfo.abbrev ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "API Average: ${apiAverage ?: "NULL"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Calculated Avg: ${calculatedAverage ?: "NULL"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Marks: $numericMarksCount numeric / $totalMarksCount total",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Absences loaded: ${absencesPerSubject.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Subject absence: ${subjectAbsence?.let { "${it.subjectName} = ${String.format("%.1f", it.getAbsencePercentage())}%" } ?: "NOT FOUND"}",
                style = MaterialTheme.typography.bodySmall
            )
            
            // Show all absence subjects for debugging
            if (absencesPerSubject.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All absence subjects:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                absencesPerSubject.take(5).forEach { absence ->
                    Text(
                        text = "  • ${absence.subjectName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun AverageCard(
    average: Double?,
    averageFormatted: String,
    markCount: Int,
    absencePercentage: Double?
) {
    val avgColor = when {
        average == null -> MaterialTheme.colorScheme.surfaceVariant
        average <= 1.5 -> MaterialTheme.colorScheme.primaryContainer
        average <= 2.5 -> MaterialTheme.colorScheme.secondaryContainer
        average <= 3.5 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    
    val avgTextColor = when {
        average == null -> MaterialTheme.colorScheme.onSurfaceVariant
        average <= 1.5 -> MaterialTheme.colorScheme.onPrimaryContainer
        average <= 2.5 -> MaterialTheme.colorScheme.onSecondaryContainer
        average <= 3.5 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    // Calculate absence color (red if > 20%, orange if > 10%, green otherwise)
    val absenceColor = when {
        absencePercentage == null -> avgTextColor
        absencePercentage > 20.0 -> MaterialTheme.colorScheme.error
        absencePercentage > 10.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = avgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Průměr",
                style = MaterialTheme.typography.titleMedium,
                color = avgTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = averageFormatted,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = avgTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$markCount známek",
                style = MaterialTheme.typography.bodyMedium,
                color = avgTextColor
            )
            
            // Absence percentage
            if (absencePercentage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    color = avgTextColor.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Absence:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = avgTextColor
                    )
                    Text(
                        text = "${String.format("%.1f", absencePercentage)}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = absenceColor
                    )
                }
            }
        }
    }
}

@Composable
fun TheoreticalMarkCalculator(
    theoreticalMark: String,
    onMarkChange: (String) -> Unit,
    theoreticalWeight: Int,
    onWeightChange: (Int) -> Unit,
    theoreticalAverage: Double?,
    currentAverage: Double?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Vyzkoušet známku",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mark input
                OutlinedTextField(
                    value = theoreticalMark,
                    onValueChange = { 
                        if (it.length <= 2) onMarkChange(it) 
                    },
                    label = { Text("Známka") },
                    placeholder = { Text("3, 2+") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                // Weight selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Váha",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (theoreticalWeight > 1) onWeightChange(theoreticalWeight - 1) },
                            modifier = Modifier.width(40.dp)
                        ) {
                            Text("-")
                        }
                        Text(
                            text = theoreticalWeight.toString(),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = { if (theoreticalWeight < 10) onWeightChange(theoreticalWeight + 1) },
                            modifier = Modifier.width(40.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }
            
            // Result display
            if (theoreticalAverage != null && currentAverage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                val difference = theoreticalAverage - currentAverage
                val diffText = when {
                    difference < -0.01 -> "o ${String.format("%.2f", -difference)} lepší"
                    difference > 0.01 -> "o ${String.format("%.2f", difference)} horší"
                    else -> "stejný"
                }
                val diffColor = when {
                    difference < -0.01 -> MaterialTheme.colorScheme.primary
                    difference > 0.01 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nový průměr: ${String.format("%.2f", theoreticalAverage)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "($diffText)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = diffColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkCard(mark: Mark) {
    val formattedDate = remember(mark.markDate) { formatDate(mark.markDate) }
    val relativeTime = remember(mark.markDate) { getRelativeTime(mark.markDate) }
    
    // Use theme as caption if caption is empty/null
    val displayCaption = when {
        !mark.caption.isNullOrBlank() -> mark.caption.trim()
        !mark.theme.isNullOrBlank() -> mark.theme.trim()
        else -> "Bez názvu"
    }
    
    // Only show theme if caption is different and not empty
    val showTheme = !mark.theme.isNullOrBlank() && 
                    !mark.caption.isNullOrBlank() && 
                    mark.theme.trim() != mark.caption.trim()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left content
            Column(modifier = Modifier.weight(1f)) {
                // Caption (main title) - uses theme if caption is empty
                Text(
                    text = displayCaption,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Theme/Subdescription under caption (only if different from caption)
                if (showTheme) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mark.theme!!.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Date with relative time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Type chip at bottom (less prominent)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = (mark.typeNote?.trim() ?: mark.type).take(30),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // Weight info if applicable
                if (!mark.isPoints && mark.weight != null && mark.weight > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Váha: ${mark.weight}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Points info if applicable
                if (mark.isPoints && mark.maxPoints != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Body: ${mark.markText}/${mark.maxPoints}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Right side - BIG mark, wider box, LEFT aligned, fills space
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = mark.markText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    color = when {
                        mark.isPoints -> MaterialTheme.colorScheme.primary
                        mark.markText in listOf("1", "1+", "1-") -> MaterialTheme.colorScheme.primary
                        mark.markText in listOf("2", "2+", "2-") -> MaterialTheme.colorScheme.secondary
                        mark.markText in listOf("3", "3+", "3-") -> MaterialTheme.colorScheme.tertiary
                        mark.markText in listOf("4", "4+", "4-") -> MaterialTheme.colorScheme.error
                        mark.markText in listOf("5", "5+", "5-") -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val date = LocalDate.parse(dateString.substringBefore("T"))
        "${date.dayOfMonth}.${date.monthValue}.${date.year}"
    } catch (e: DateTimeParseException) {
        dateString.substringBefore("T")
    }
}

private fun getRelativeTime(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime = LocalDateTime.parse(dateString, formatter)
        val now = LocalDateTime.now()
        
        val daysBetween = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())
        
        when {
            daysBetween == 0L -> "Dnes"
            daysBetween == 1L -> "Včera"
            daysBetween < 7 -> "$daysBetween dny"
            daysBetween < 14 -> "Před týdnem"
            daysBetween < 30 -> "${daysBetween / 7} týdny"
            daysBetween < 60 -> "Před měsícem"
            daysBetween < 365 -> "${daysBetween / 30} měsíce"
            else -> "${daysBetween / 365} roky"
        }
    } catch (e: Exception) {
        ""
    }
}

private fun calculateSubjectAverage(marks: List<Mark>): Double? {
    val numericMarks = marks.filter { !it.isPoints }
    if (numericMarks.isEmpty()) return null
    
    var totalWeight = 0
    var weightedSum = 0.0
    
    for (mark in numericMarks) {
        val value = parseMarkValueForDetail(mark.markText)
        if (value != null) {
            val weight = mark.weight ?: 1
            totalWeight += weight
            weightedSum += value * weight
        }
    }
    
    return if (totalWeight > 0) weightedSum / totalWeight else null
}

private fun parseMarkValueForDetail(markText: String): Double? {
    return when (markText.trim()) {
        "1" -> 1.0
        "1+" -> 1.3
        "1-" -> 1.7
        "2" -> 2.0
        "2+" -> 2.3
        "2-" -> 2.7
        "3" -> 3.0
        "3+" -> 3.3
        "3-" -> 3.7
        "4" -> 4.0
        "4+" -> 4.3
        "4-" -> 4.7
        "5" -> 5.0
        "5+" -> 5.3
        "5-" -> 5.7
        else -> markText.replace(",", ".").toDoubleOrNull()
    }
}

private fun parseMarkValueForCalculator(markText: String): Double? {
    if (markText.isBlank()) return null
    return parseMarkValueForDetail(markText)
}

private fun calculateTheoreticalAverage(
    existingMarks: List<Mark>,
    theoreticalMarkValue: Double,
    theoreticalWeight: Int
): Double {
    val numericMarks = existingMarks.filter { !it.isPoints }
    
    var totalWeight = theoreticalWeight
    var weightedSum = theoreticalMarkValue * theoreticalWeight
    
    for (mark in numericMarks) {
        val value = parseMarkValueForDetail(mark.markText)
        if (value != null) {
            val weight = mark.weight ?: 1
            totalWeight += weight
            weightedSum += value * weight
        }
    }
    
    return if (totalWeight > 0) weightedSum / totalWeight else theoreticalMarkValue
}
