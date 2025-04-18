package org.moraveco.omleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.Key.Companion.Settings
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import omleditor.composeapp.generated.resources.Res
import omleditor.composeapp.generated.resources.oml
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory
import org.fife.ui.rtextarea.RTextScrollPane
import org.jetbrains.compose.resources.painterResource
import java.awt.FileDialog
import java.awt.Font
import java.awt.Frame
import java.io.File
import java.io.IOException
import javax.swing.JOptionPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.concurrent.thread

// Main application entry point

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose Code Editor",
        state = windowState,
        icon = painterResource(Res.drawable.oml)
    ) {
        MenuBar {
            Menu("File") {
                Item("New", onClick = {
                    // New file action will be handled in the component
                    eventBus.emit(EditorEvent.NewFile)
                })
                Item("Open", onClick = {
                    eventBus.emit(EditorEvent.OpenFile)
                })
                Item("Save", onClick = {
                    eventBus.emit(EditorEvent.SaveFile)
                })
                Item("Save As", onClick = {
                    eventBus.emit(EditorEvent.SaveFileAs)
                })
                Separator()
                Item("Exit", onClick = ::exitApplication)
            }
            Menu("Edit") {
                Item("Undo", onClick = {
                    eventBus.emit(EditorEvent.Undo)
                })
                Item("Redo", onClick = {
                    eventBus.emit(EditorEvent.Redo)
                })
                Separator()
                Item("Cut", onClick = {
                    eventBus.emit(EditorEvent.Cut)
                })
                Item("Copy", onClick = {
                    eventBus.emit(EditorEvent.Copy)
                })
                Item("Paste", onClick = {
                    eventBus.emit(EditorEvent.Paste)
                })
            }
            Menu("Run") {
                Item("Run Code", onClick = {
                    eventBus.emit(EditorEvent.RunCode)
                })
            }
        }

        MaterialTheme {
            CodeEditorApp()
        }
    }
}

object eventBus {
    private val listeners = mutableListOf<(EditorEvent) -> Unit>()

    fun subscribe(listener: (EditorEvent) -> Unit) {
        listeners.add(listener)
    }

    fun emit(event: EditorEvent) {
        listeners.forEach { it(event) }
    }
}

// Events for the editor
sealed class EditorEvent {
    data object NewFile : EditorEvent()
    data object OpenFile : EditorEvent()
    data object SaveFile : EditorEvent()
    data object SaveFileAs : EditorEvent()
    data object Undo : EditorEvent()
    data object Redo : EditorEvent()
    data object Cut : EditorEvent()
    data object Copy : EditorEvent()
    data object Paste : EditorEvent()
    data object RunCode : EditorEvent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorApp() {
    var filePath by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("// Write your code here") }
    var consoleOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val settings: PreferencesSettings = PreferencesSettings.Factory().create("settings")

    fun saveLastOpenedFile(path: String) {
        settings.putString("last_file", path)
    }

    fun loadLastOpenedFile(): String? {
        return settings.getStringOrNull("last_file")
    }

    LaunchedEffect(Unit) {


        val lastOpened = loadLastOpenedFile()
        if (lastOpened != null) {
            try {
                val content = File(lastOpened).readText()
                fileContent = content
                filePath = lastOpened // ✅ set the path here!
            } catch (e: IOException) {
                fileContent = "// Could not load last file: ${e.message}"
            }
        } else {
            fileContent = "// Write your code here"
        }
        eventBus.subscribe { event ->
            when (event) {
                EditorEvent.NewFile -> {
                    fileContent = "// New file"
                    filePath = null
                }
                EditorEvent.OpenFile -> {
                    val currentFilePath = openFileDialog()
                    currentFilePath?.let {
                        try {
                            val content = File(it).readText()
                            fileContent = content
                            filePath = it
                            saveLastOpenedFile(it) // ✅ persist after opening
                        } catch (e: IOException) {
                            JOptionPane.showMessageDialog(null, "Error reading file: ${e.message}")
                        }
                    }
                }
                EditorEvent.SaveFile -> {
                    val currentFilePath = filePath ?: saveFileDialog()
                    currentFilePath?.let {
                        try {
                            File(it).writeText(fileContent)
                            filePath = it
                        } catch (e: IOException) {
                            JOptionPane.showMessageDialog(null, "Error saving file: ${e.message}")
                        }
                    }
                }
                EditorEvent.SaveFileAs -> {
                    val currentFilePath = saveFileDialog()
                    currentFilePath?.let {
                        try {
                            File(it).writeText(fileContent)
                            filePath = it
                        } catch (e: IOException) {
                            JOptionPane.showMessageDialog(null, "Error saving file: ${e.message}")
                        }
                    }
                }
                EditorEvent.RunCode -> {
                    if (!isRunning) {
                        isRunning = true
                        consoleOutput = "Running...\n"

                        // Simulate code execution in a separate thread
                        thread {
                            try {
                                // This is just a simulation. In a real application, you would execute the code here.
                                Thread.sleep(1000) // Simulate processing time
                                consoleOutput += runLexerParser(filePath ?: "/Users/macos/CLionProjects/OML/newMain.oml")
                            } catch (e: Exception) {
                                consoleOutput += "Error: ${e.message}\n"
                            } finally {
                                isRunning = false
                            }
                        }
                    }
                }
                else -> {
                    // Handle other events like Undo, Redo, Cut, Copy, Paste
                    // These would need more complex implementation
                }
            }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.isMetaPressed && keyEvent.key == Key.S) {
                    val currentFilePath = filePath ?: saveFileDialog()
                    currentFilePath?.let {
                        try {
                            File(it).writeText(fileContent)
                            filePath = it
                        } catch (e: IOException) {
                            JOptionPane.showMessageDialog(null, "Error saving file: ${e.message}")
                        }

                    }
                    true
                } else {
                    false // Ignore event
                }
            }
    ) {
        TopAppBar(
            title = { Text("Code Editor - ${filePath ?: "Untitled"}") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(39,49,52),
                titleContentColor = Color.White,
            ),
            actions = {
                IconButton(
                    onClick = {
                        if (!isRunning) {
                            isRunning = true
                            consoleOutput = "Running...\n"

                            thread {
                                try {
                                    // This is just a simulation. In a real application, you would execute the code here.
                                    Thread.sleep(1000) // Simulate processing time
                                    consoleOutput += runLexerParser(filePath ?: "/Users/macos/CLionProjects/OML/newMain.oml")
                                } catch (e: Exception) {
                                    consoleOutput += "Error: ${e.message}\n"
                                } finally {
                                    isRunning = false
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = if (isRunning) Color.Gray else Color.Green
                    )
                }
            }
        )

        Row(modifier = Modifier.weight(1f)) {
            EditorWithSwing(
                text = fileContent,
                onTextChanged = { fileContent = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        ConsolePanel(consoleOutput, isRunning)
    }
}

@Composable
fun EditorWithSwing(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TokenMakerFactory.setDefaultInstance(object : TokenMakerFactory() {
        override fun getTokenMakerImpl(key: String?): TokenMaker? {
            return if (key == "text/oml") {
                OMLTokenMaker()
            } else {
                null
            }
        }

        override fun keySet(): Set<String> {
            return setOf("text/oml")
        }
    })




    SwingPanel(
        modifier = modifier,
        factory = {
            val editor = RSyntaxTextArea().apply {
                syntaxEditingStyle = "text/oml"
                isCodeFoldingEnabled = true
                font = Font("JetBrains Mono", Font.PLAIN, 14)
                tabSize = 4
                setText(text)
                caretPosition = 0
            }
            try {
                val theme = Theme.load(Theme::class.java.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"))
                theme.apply(editor)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            editor.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = update()
                override fun removeUpdate(e: DocumentEvent?) = update()
                override fun changedUpdate(e: DocumentEvent?) = update()
                private fun update() {
                    onTextChanged(editor.text)
                }
            })

            RTextScrollPane(editor)
        },
        update = { panel ->
            val editor = panel.textArea
            if (editor.text != text) {
                try {
                    editor.text = text
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }

            }
        }
    )
}

@Composable
fun ConsolePanel(output: String, isRunning: Boolean) {
    Column(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .background(Color(0xFF2B2B2B))
    ) {
        Text("Console", color = Color.White, modifier = Modifier.padding(8.dp))
        Divider(color = Color.DarkGray)

        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text(
                text = output,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )

            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}


// File dialog to open a file
fun openFileDialog(): String? {
    val fileDialog = FileDialog(Frame(), "Open a file", FileDialog.LOAD)
    fileDialog.isVisible = true

    return if (fileDialog.file != null) {
        "${fileDialog.directory}${fileDialog.file}"
    } else {
        null
    }
}

// File dialog to save a file
fun saveFileDialog(): String? {
    val fileDialog = FileDialog(Frame(), "Save a file", FileDialog.SAVE)
    fileDialog.isVisible = true

    return if (fileDialog.file != null) {
        "${fileDialog.directory}${fileDialog.file}"
    } else {
        null
    }
}

fun runLexerParser(filePath: String): String {
    return try {
        // Replace "my_lexer_parser" with the actual path to your lexer/parser executable
        val omlPath = File("src/interpreter/OML").absolutePath
        val process = ProcessBuilder(omlPath, filePath)
            .redirectErrorStream(true) // Combine stdout and stderr
            .start()


        // Capture the output (lexer/parser result)
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor() // Wait for the process to complete
        output
    } catch (e: Exception) {
        "Error running lexer/parser: ${e.message}"
    }
}