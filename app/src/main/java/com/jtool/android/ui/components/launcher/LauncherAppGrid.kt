package com.jtool.android.ui.components.launcher

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.ui.navigation.AppProperties

@Composable
fun LauncherAppGrid(
    visibleApps: List<AppProperties>,
    hiddenAppsState: List<String>,
    gridColumnCount: Int,
    isEditMode: Boolean,
    clickHandlers: Map<AppScreenEnum, () -> Unit>,
    onToggleVisibility: (String) -> Unit,
    onEnableEditMode: () -> Unit,
    onSwapApps: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState()
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(gridColumnCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !isEditMode
    ) {
        itemsIndexed(
            items = visibleApps,
            key = { _, app -> app.id.id }
        ) { index, app ->
            val isHidden = hiddenAppsState.contains(app.id.id)
            val onClick = if (isEditMode) {
                {} 
            } else {
                clickHandlers[app.id] ?: {}
            }

            val isSideBySide = gridColumnCount <= 2

            var dragX by remember { mutableFloatStateOf(0f) }
            var dragY by remember { mutableFloatStateOf(0f) }
            var itemSize by remember { mutableStateOf(IntSize.Zero) }

            val dragModifier = if (isEditMode) {
                Modifier
                    .onGloballyPositioned { itemSize = it.size }
                    .pointerInput(Unit, index) {
                        detectDragGestures(
                            onDragStart = {
                                draggingIndex = index
                                dragX = 0f
                                dragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX += dragAmount.x
                                dragY += dragAmount.y
                            },
                            onDragEnd = {
                                if (draggingIndex == index && itemSize.width > 0 && itemSize.height > 0) {
                                    val totalWidth = itemSize.width + 12.dp.toPx()
                                    val totalHeight = itemSize.height + 16.dp.toPx()
                                    
                                    val colOffset = (dragX / totalWidth).roundToInt()
                                    val rowOffset = (dragY / totalHeight).roundToInt()
                                    
                                    val targetIndex = (index + colOffset + (rowOffset * gridColumnCount)).coerceIn(0, visibleApps.size - 1)
                                    
                                    if (targetIndex != index) {
                                        onSwapApps(index, targetIndex)
                                    }
                                }
                                draggingIndex = null
                                dragX = 0f
                                dragY = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragX = 0f
                                dragY = 0f
                            }
                        )
                    }
            } else {
                Modifier
            }

            val isDragging = draggingIndex == index
            val zIndex = if (isDragging) 1f else 0f
            
            val offset = if (isDragging) {
                IntOffset(dragX.roundToInt(), dragY.roundToInt())
            } else {
                IntOffset.Zero
            }

            Box(
                modifier = Modifier
                    .animateItem()
                    .zIndex(zIndex)
                    .graphicsLayer {
                        translationX = offset.x.toFloat()
                        translationY = offset.y.toFloat()
                        scaleX = if (isDragging) 1.05f else 1f
                        scaleY = if (isDragging) 1.05f else 1f
                        alpha = if (isDragging) 0.8f else 1f
                    }
                    .fillMaxWidth()
                    .then(dragModifier)
            ) {
                if (isSideBySide) {
                    LauncherSideBySideCard(
                        name = app.getName(),
                        icon = app.icon!!,
                        isEditMode = isEditMode,
                        isHidden = isHidden,
                        onClick = onClick,
                        onLongClick = onEnableEditMode,
                        onToggleVisibility = { onToggleVisibility(app.id.id) }
                    )
                } else {
                    LauncherStackedCard(
                        name = app.getName(),
                        icon = app.icon!!,
                        isEditMode = isEditMode,
                        isHidden = isHidden,
                        onClick = onClick,
                        onLongClick = onEnableEditMode,
                        onToggleVisibility = { onToggleVisibility(app.id.id) }
                    )
                }
            }
        }
    }
}
