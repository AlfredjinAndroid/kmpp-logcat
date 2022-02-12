// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import viewModel.MainViewModel
import java.io.File
import java.nio.file.FileSystem
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.absolutePathString

@Composable
@Preview
fun App() {
    val mainViewModel = MainViewModel()
    val coroutineScope = rememberCoroutineScope()
    val fileChooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    LaunchedEffect(Unit) {
        mainViewModel.initScope(coroutineScope)
    }

    DisposableEffect(Unit) {
        onDispose { mainViewModel.release() }
    }
    MaterialTheme {
        Scaffold() {
            val devices by mainViewModel.devices.collectAsState(emptyList())
            val isLogging by mainViewModel.isLogging.collectAsState(false)
            val logList by mainViewModel.logList.collectAsState(emptyList())
            val adbPath by mainViewModel.adbPath.collectAsState("")
            val logPath by mainViewModel.logPath.collectAsState(FileSystemView.getFileSystemView().homeDirectory.absolutePath)
            val logState = rememberLazyListState()
            LaunchedEffect(logList) {
                coroutineScope.launch {
                    logState.scrollToItem(logList.count())
                }
            }
            LaunchedEffect(adbPath) {
                mainViewModel.refreshDevices()
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ADB Path")
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp)
                        .background(color = Color(0xFFEFF0F6))
                        .clickable {
                            fileChooser.showOpenDialog(null)
                            fileChooser.selectedFile?.absolutePath?.also {
                                mainViewModel.adbPath(it)
                            }
                        }) {
                        Text(adbPath.ifEmpty { "选择路径" }, modifier = Modifier.padding(16.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Log Path")
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp)
                        .background(color = Color(0xFFEFF0F6))
                        .clickable {

                            fileChooser.showOpenDialog(null)
                            fileChooser.selectedFile?.absolutePath?.also {
                                mainViewModel.logPath(it)
                            }
                        }) {
                        Text(logPath, modifier = Modifier.padding(16.dp))
                    }
                }
                LazyColumn {
                    items(devices) { device ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = device.name, modifier = Modifier.weight(1f))
                                Text(text = device.sid, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    if (isLogging) {
                                        mainViewModel.stopLog()
                                    } else {
                                        mainViewModel.logcat(device)
                                    }
                                }) {
                                    Text(if (isLogging) "搜集中" else "搜集")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(onClick = {
                                    mainViewModel.clear()
                                }) {
                                    Text("清空")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (logList.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp).background(color = Color.Gray)) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), state = logState) {
                            items(logList) { log ->
                                Text(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Locale.setDefault(Locale.US)
    Window(onCloseRequest = ::exitApplication, title = "Alfred Test Tools") {
        App()
    }
}
