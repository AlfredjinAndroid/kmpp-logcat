import java.io.BufferedReader
import java.io.InputStreamReader

fun String.execute(): Process {
    val runtime = Runtime.getRuntime()
    return runtime.exec(this.split(" ").filter { f-> f.isNotEmpty() }.toTypedArray())
}


fun Process.text(): String = InputStreamReader(inputStream).readText()

fun Process.listener(callback: (String) -> Unit) {
    callback("Start...")
    try {
        val bufReader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (bufReader.readLine().also { line = it } != null) {
            line?.also { callback(it) }
        }

        val errorReader = BufferedReader(InputStreamReader(errorStream))
        var error: String?
        while (errorReader.readLine().also { error = it } != null) {
            error?.also(callback)
        }
    } catch (e: Exception) {
        println(e.message)
    }
}
