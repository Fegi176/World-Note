package app.nodenote.worldbuilder.data

import app.nodenote.core.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.*

enum class RestoreCheckpoint {
    BEFORE_PROMOTION,
    AFTER_PROMOTION,
    BEFORE_DATABASE,
    AFTER_DATABASE,
}

/**
 * Durable file promotion and atomic SQL commit shared by production and failure-injection tests.
 */
class RestoreEngine(private val repo: Repository, private val files: ManagedFiles) {
    suspend fun restore(
        source: List<WorldBundle>,
        archive: InspectedArchive?,
        replace: Boolean = false,
        beforeCommit: () -> Unit = {},
        afterCommit: () -> Unit = {},
        checkpoint: (RestoreCheckpoint) -> Unit = {},
    ): List<WorldBundle> =
        withContext(Dispatchers.IO) {
            require(source.isNotEmpty()) { "Archive contains no worlds" }
            val copies =
                if (replace) source
                else source.map { Remap.world(it, it.world.name + " (restored)") }
            if (replace) {
                val old =
                    repo.workspace().worlds.filter { b -> copies.any { it.world.id == b.world.id } }
                require(old.isNotEmpty()) { "No matching existing world; import as new worlds" }
                files.safetyCopy(Workspace(old), "restore")
            }
            val promoted = mutableListOf<File>()
            val journal = File(files.recovery, "restore-${newId()}.journal")
            var committed = false
            try {
                checkpoint(RestoreCheckpoint.BEFORE_PROMOTION)
                source.zip(copies).forEach { (old, new) ->
                    old.records
                        .zip(new.records)
                        .filter { it.first.kind == Kind.ATTACHMENT }
                        .forEach { (a, b) ->
                            currentCoroutineContext().ensureActive()
                            val input =
                                archive?.files?.get("worlds/${old.world.id}/${a.f("path")}")
                                    ?: error("Missing staged asset")
                            val target = repo.asset(b)
                            if (target.exists()) {
                                require(sha256(target) == b.f("sha256")) {
                                    "Conflicting immutable attachment identity"
                                }
                            } else {
                                promoted += target
                                FileOutputStream(journal).use { out ->
                                    out.write(
                                        promoted
                                            .joinToString("\n") { it.name }
                                            .toByteArray(Charsets.UTF_8)
                                    )
                                    out.fd.sync()
                                }
                                input.inputStream().use { stream ->
                                    FileOutputStream(target).use { out ->
                                        val buffer = ByteArray(65536)
                                        while (true) {
                                            currentCoroutineContext().ensureActive()
                                            val n = stream.read(buffer)
                                            if (n < 0) break
                                            out.write(buffer, 0, n)
                                        }
                                        out.fd.sync()
                                    }
                                }
                            }
                            require(sha256(target) == b.f("sha256")) { "Attachment hash mismatch" }
                            files.thumbnail(b)
                            checkpoint(RestoreCheckpoint.AFTER_PROMOTION)
                        }
                }
                checkpoint(RestoreCheckpoint.BEFORE_DATABASE)
                currentCoroutineContext().ensureActive()
                beforeCommit()
                withContext(NonCancellable) {
                    if (replace) repo.replaceMany(copies) else repo.insertMany(copies)
                    committed = true
                    afterCommit()
                    checkpoint(RestoreCheckpoint.AFTER_DATABASE)
                    journal.delete()
                }
                copies
            } catch (e: Exception) {
                if (!committed) {
                    promoted.forEach { file ->
                        file.delete()
                        files.removeThumbnail(file.name)
                    }
                    journal.delete()
                }
                // A post-commit interruption leaves a journal; startup protects all committed
                // identities.
                throw e
            }
        }

    suspend fun recoverJournals() =
        withContext(Dispatchers.IO) {
            val retained = repo.attachmentIds()
            files.recovery
                .listFiles { f -> f.extension == "journal" }
                ?.forEach { journal ->
                    journal
                        .readLines()
                        .filter { Regex("[a-fA-F0-9-]{36}").matches(it) && it !in retained }
                        .forEach { id ->
                            File(repo.assetRoot, id).delete()
                            files.removeThumbnail(id)
                        }
                    journal.delete()
                }
        }
}
