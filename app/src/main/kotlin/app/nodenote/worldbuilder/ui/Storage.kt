package app.nodenote.worldbuilder.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nodenote.worldbuilder.AppModel
import app.nodenote.worldbuilder.R
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StorageScreen(vm: AppModel) {
    var revision by remember { mutableIntStateOf(0) }
    var archives by remember { mutableStateOf(emptyList<File>()) }
    var bytes by remember { mutableLongStateOf(0L) }
    var remove by remember { mutableStateOf<File?>(null) }
    var export by remember { mutableStateOf<File?>(null) }
    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            val source = export
            if (uri != null && source != null)
                vm.action {
                    withContext(Dispatchers.IO) {
                        vm.getApplication<android.app.Application>()
                            .contentResolver
                            .openOutputStream(uri, "wt")!!
                            .use { output ->
                                source.inputStream().use { it.copyTo(output) }
                            }
                    }
                    vm.message.value = "Recovery archive exported"
                }
        }
    LaunchedEffect(revision) {
        withContext(Dispatchers.IO) {
            archives = vm.files.recoveryArchives()
            bytes = vm.files.thumbnailBytes()
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Heading(
                stringResource(R.string.atlas_4a13d2dea8),
                "Manage previews and safety copies stored on this device.",
            )
            Text(
                "Cached previews · ${android.text.format.Formatter.formatFileSize(vm.getApplication(), bytes)}"
            )
            Text(
                stringResource(R.string.atlas_6e0d283c90),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    vm.action {
                        withContext(Dispatchers.IO) { vm.files.clearThumbnails() }
                        revision++
                    }
                }
            ) {
                Text(stringResource(R.string.atlas_75d78d36a8))
            }
            Heading(
                stringResource(R.string.atlas_33799afe66),
                "Created before replacement or permanent deletion. Export a copy before removing it if you may need it again.",
            )
            if (archives.isEmpty()) Empty(stringResource(R.string.atlas_3b40a894d7))
        }
        items(archives, key = { it.name }) { file ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        file.name.substringAfter("pre-").substringBefore('-').replaceFirstChar {
                            it.uppercase()
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(DateFormat.getDateTimeInstance().format(Date(file.lastModified())))
                    Text(
                        android.text.format.Formatter.formatFileSize(
                            vm.getApplication(),
                            file.length(),
                        )
                    )
                    ActionRow {
                        TextButton(
                            onClick = {
                                export = file
                                picker.launch(file.name)
                            }
                        ) {
                            Text(stringResource(R.string.atlas_85eb8abc7a))
                        }
                        TextButton(onClick = { vm.inspect(Uri.fromFile(file)) }) {
                            Text(stringResource(R.string.atlas_fcf3c942a4))
                        }
                        TextButton(onClick = { remove = file }) {
                            Text(stringResource(R.string.atlas_e963907dac))
                        }
                    }
                }
            }
        }
    }
    remove?.let { file ->
        AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text(stringResource(R.string.atlas_f12388d970)) },
            text = {
                Text(
                    "This permanently removes ${file.name}. Your current worlds and original images stay saved."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        remove = null
                        vm.action {
                            withContext(Dispatchers.IO) { vm.files.removeRecoveryArchive(file) }
                            revision++
                        }
                    }
                ) {
                    Text(stringResource(R.string.atlas_4f1f63742c))
                }
            },
            dismissButton = {
                TextButton(onClick = { remove = null }) {
                    Text(stringResource(R.string.atlas_744f735032))
                }
            },
        )
    }
}
