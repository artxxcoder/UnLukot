package com.unlukot.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlukot.app.ai.Kind
import com.unlukot.app.ai.ScanResult
import com.unlukot.app.data.ResultTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(result: ScanResult, image: Bitmap, showBoxes: Boolean, initialTab: Int, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var tab by rememberSaveable { mutableStateOf(ResultTab.entries[initialTab]) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var kindFilter by remember { mutableStateOf<Kind?>(null) }
    var boxesOn by rememberSaveable { mutableStateOf(showBoxes) }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val json = remember(result) { result.toJson().toString(2) }
    val kinds = remember(result) { result.components.groupingBy { it.kind }.eachCount().toList() }

    fun copy(text: String, what: String) {
        clipboard.setText(AnnotatedString(text))
        scope.launch { snackbar.showSnackbar("$what copied") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan result", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { copy(result.toPlainText(), "Result") }) { Icon(Icons.Filled.ContentCopy, "Copy all") }
                    IconButton(onClick = { share(ctx, result.toPlainText()) }) { Icon(Icons.Filled.Share, "Share") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SummaryCard(result) }
            item {
                ScannedPage(
                    bitmap = image,
                    components = result.components,
                    selected = selected,
                    showBoxes = boxesOn,
                    onToggleBoxes = { boxesOn = !boxesOn },
                    onSelect = { i -> selected = i; if (i != null) { tab = ResultTab.COMPONENTS; kindFilter = null } }
                )
            }
            item { PillSelector(ResultTab.entries, tab, { it.label }, { tab = it }) }
            when (tab) {
                ResultTab.COMPONENTS -> {
                    if (result.components.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Filled.TextSnippet, "No readable text",
                                "Try brighter, even light, hold the phone steady, or retake the photo."
                            )
                        }
                    } else if (kinds.size > 1) {
                        item {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TypeChip("All ${result.components.size}", kindFilter == null) { kindFilter = null }
                                kinds.forEach { (k, n) ->
                                    TypeChip("${k.label} $n", kindFilter == k) { kindFilter = if (kindFilter == k) null else k }
                                }
                            }
                        }
                    }
                    itemsIndexed(result.components) { i, c ->
                        if (kindFilter == null || c.kind == kindFilter) {
                            ComponentRow(c, selected == i) { selected = if (selected == i) null else i }
                        }
                    }
                }
                ResultTab.TEXT -> item {
                    CopyableBlock(result.rawText.ifBlank { "(no text)" }, mono = false) { copy(result.rawText, "Text") }
                }
                ResultTab.JSON -> item {
                    CopyableBlock(json, mono = true) { copy(json, "JSON") }
                }
            }
        }
    }
}

@Composable
private fun CopyableBlock(text: String, mono: Boolean, onCopy: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SelectionContainer {
                Text(
                    text,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    fontSize = if (mono) 12.sp else 15.sp,
                    lineHeight = if (mono) 18.sp else 23.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                        .then(if (mono) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                )
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (mono) "Copy JSON" else "Copy text")
            }
        }
    }
}

private fun share(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Share scan"))
}
