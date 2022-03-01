package viewModel

import execute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import listener
import pojo.AndroidDevices
import text
import java.io.FileOutputStream
import java.nio.file.Paths
import java.sql.Time
import java.util.Calendar
import java.util.Date
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.absolutePathString

class MainViewModel {

    private val _devices = MutableStateFlow<List<AndroidDevices>>(value = emptyList())
    val devices: Flow<List<AndroidDevices>> = _devices

    private val _currentDevice = MutableStateFlow<AndroidDevices?>(value = null)
    val currentDevice: Flow<AndroidDevices?> = _currentDevice

    private val _logProcess = MutableStateFlow<Process?>(value = null)
    val isLogging: Flow<Boolean> = _logProcess.map { f -> f?.isAlive ?: false }

    private val _logList = MutableStateFlow<List<String>>(value = emptyList())
    val logList: Flow<List<String>> = _logList

    private val _adbPath = MutableStateFlow(value = "")
    val adbPath:StateFlow<String> = _adbPath

    private val _logPath = MutableStateFlow(value = FileSystemView.getFileSystemView().homeDirectory.absolutePath)
    val logPath:StateFlow<String> = _logPath


    private var viewModelScope: CoroutineScope? = null
    fun initScope(scope: CoroutineScope) {
        viewModelScope = scope
    }

    fun release() {
        viewModelScope?.cancel()
        viewModelScope = null
    }

    fun adbPath(path:String) {
        _adbPath.value = path
    }

    fun logPath(path: String) {
        _logPath.value = path
    }


    fun refreshDevices() {
        val adbP = _adbPath.value
        if (adbP.isEmpty()) return
        viewModelScope?.launch(Dispatchers.IO) {
            _logProcess.value?.destroy()
            val devicesProgress = "$adbP/adb devices -l".execute()
            val result = devicesProgress.text()

            result.split("\n").filter { f -> f.contains("model") }.map { line ->
                val infos = line.split(" ")
                val sId = infos.first()
                val model = infos.find { f -> f.contains("model") }?.split(":")?.last() ?: "unknown"
                AndroidDevices(name = model, sid = sId)
            }.also {
                _devices.emit(it)
            }
        }
    }

    ///Users/lilijie/developer/android_sdk/platform-tools/
    fun logcat(androidDevices: AndroidDevices) {
        toggleFlush()
        val adbP = _adbPath.value
        if (adbP.isEmpty()) return
        _currentDevice.value = androidDevices
        viewModelScope?.launch(Dispatchers.IO) {
            val logcatProcess = """$adbP/adb -s ${androidDevices.sid} logcat""".execute()
            _logProcess.emit(logcatProcess)
            logcatProcess.listener {
                fos?.write(it.toByteArray())
                fos?.write("\n".toByteArray())
                fos?.flush()
                viewModelScope?.launch {
                    _logList.emit(_logList.value + it)
                }
            }
        }
    }

    fun clear() {
        _logList.value = emptyList()
    }

    fun testLog(msg:String){
        _logList.value = _logList.value + msg
    }

    fun stopLog() {
        viewModelScope?.launch(Dispatchers.IO) {
            _logProcess.value?.destroy()
            _logProcess.emit(null)
        }
        toggleFlush()
    }

    private var fos: FileOutputStream? = null
    private fun toggleFlush() {
        fos = if (fos == null) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR)
            val minute = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            val filePath = _logPath.value//FileSystemView.getFileSystemView().homeDirectory.absolutePath
            val fileName = String.format("$filePath/%02d-%02d-%02d-%02d-%02d-%02d-Android.log",year,month,day,hour,minute,seconds)
            FileOutputStream(fileName)
        } else {
            fos?.flush()
            fos?.close()
            null
        }
    }
}