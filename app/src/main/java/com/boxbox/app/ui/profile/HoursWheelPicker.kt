package com.boxbox.app.ui.profile

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxbox.app.ui.theme.AppColors
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A clock-style wheel picker for selecting how many hours between race countdown
 * notifications, meant to live INSIDE a confirm/cancel dialog (see
 * NotificationIntervalDialog below) rather than inline in a scrolling list - an inline
 * wheel is too easy to nudge by accident while scrolling the rest of the screen, so the
 * picker only takes effect when the user explicitly taps Save.
 */
@Composable
fun HoursWheelPicker(
    selectedHours: Int,
    onHoursChanged: (Int) -> Unit,
    minHours: Int = 1,
    maxHours: Int = 12
) {
    val hours = remember(minHours, maxHours) { (minHours..maxHours).toList() }
    val itemHeight = 40.dp
    val visibleItems = 3 // one item above, the centered one, and one below

    val initialIndex = hours.indexOf(selectedHours).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (!isScrolling) {
                    val centeredIndex = listState.firstVisibleItemIndex
                    hours.getOrNull(centeredIndex)?.let { onHoursChanged(it) }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(hours) { index, hourValue ->
                val isSelected = index == listState.firstVisibleItemIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hourValue == 1) "1 hour" else "$hourValue hours",
                        color = if (isSelected) AppColors.primary else AppColors.onSurfaceVariant,
                        fontSize = if (isSelected) 18.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog wrapping HoursWheelPicker with explicit Save/Cancel - this is the actual
 * entry point to use from ProfileScreen. The wheel inside freely scrolls as the user
 * drags, but nothing is persisted (onSave isn't called) unless they tap Save; tapping
 * outside or Cancel discards the in-progress selection entirely.
 */
@Composable
fun NotificationIntervalDialog(
    currentHours: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var pendingHours by remember { mutableStateOf(currentHours) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Interval", color = AppColors.onBackground) },
        text = {
            Column {
                Text(
                    "Notify me every",
                    color = AppColors.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                HoursWheelPicker(
                    selectedHours = pendingHours,
                    onHoursChanged = { pendingHours = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pendingHours) }) {
                Text("Save", color = AppColors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.onSurfaceVariant)
            }
        },
        containerColor = AppColors.surface
    )
}