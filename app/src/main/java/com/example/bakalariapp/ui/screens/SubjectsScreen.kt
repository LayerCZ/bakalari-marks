package com.example.bakalariapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bakalariapp.data.model.Subject
import com.example.bakalariapp.ui.viewmodel.MarksUiState
import com.example.bakalariapp.ui.viewmodel.MarksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: MarksViewModel,
    onSubjectClick: (Subject) -> Unit,
    onLogout: () -> Unit
) {
    val uiState = viewModel.uiState
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    
    // Track refresh animation state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Animate refresh icon rotation
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing || uiState.isLoading) 360f else 0f,
        animationSpec = if (isRefreshing || uiState.isLoading) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            )
        } else {
            tween(300)
        },
        label = "refresh_rotation"
    )
    
    // Show toast on error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }
    
    // Handle pull-to-refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            isRefreshing = true
            viewModel.loadMarks(forceRefresh = true)
            pullToRefreshState.endRefresh()
            isRefreshing = false
        }
    }
    
    // Sync local refresh state with ViewModel loading state
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Známky") },
                actions = {
                    IconButton(
                        onClick = { 
                            isRefreshing = true
                            viewModel.loadMarks(forceRefresh = true)
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh",
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                    // Logout hidden for now
                    // IconButton(onClick = { viewModel.logout(onLogout) }) {
                    //     Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    // }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.subjects.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.subjects.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Chyba: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            IconButton(
                                onClick = { 
                                    isRefreshing = true
                                    viewModel.loadMarks(forceRefresh = true)
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            }
                        }
                    }
                }
                uiState.subjects.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Žádné známky k zobrazení")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.subjects,
                            key = { it.subjectInfo.id }
                        ) { subject ->
                            SubjectCard(
                                subject = subject,
                                onClick = { onSubjectClick(subject) }
                            )
                        }
                    }
                }
            }
            
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState
            )
        }
    }
}

@Composable
fun SubjectCard(
    subject: Subject,
    onClick: () -> Unit
) {
    // Parse average once with remember
    val (average, averageText) = remember(subject.averageText) {
        val raw = subject.averageText?.trim() ?: ""
        // Handle formats like "2,86 " or "2.86" or "3,00 "
        val num = raw.replace(",", ".").replace(" ", "").toDoubleOrNull()
        val text = if (num != null) String.format("%.2f", num) else "-"
        Pair(num, text)
    }
    
    val markCount = subject.marks.size
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.subjectInfo.name.trim(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$markCount známek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Average with fixed width - LEFT ALIGNED, moved right
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = averageText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    color = when {
                        average == null -> MaterialTheme.colorScheme.onSurface
                        average <= 1.5 -> MaterialTheme.colorScheme.primary
                        average <= 2.5 -> MaterialTheme.colorScheme.secondary
                        average <= 3.5 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
