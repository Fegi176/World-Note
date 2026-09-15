package app.nodenote.worldbuilder.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import app.nodenote.worldbuilder.AppModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Saveable for recreation, durable preferences for a fresh process. Never writes defaults on read.
 */
@Composable
fun <T> rememberSetting(
    vm: AppModel,
    key: String,
    initial: T,
    decode: (String) -> T,
    encode: (T) -> String,
): MutableState<T> {
    val local =
        rememberSaveable(key) {
            mutableStateOf(
                vm.prefs.value[key]?.let { runCatching { decode(it) }.getOrDefault(initial) }
                    ?: initial
            )
        }
    return remember(vm, key, local) {
        object : MutableState<T> {
            override var value: T
                get() = local.value
                set(value) {
                    local.value = value
                    vm.preference(key, encode(value))
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberText(vm: AppModel, key: String, initial: String = "") =
    rememberSetting(vm, key, initial, { it }, { it })

@Composable
fun rememberFlag(vm: AppModel, key: String, initial: Boolean = false) =
    rememberSetting(vm, key, initial, { it.toBooleanStrict() }, { it.toString() })

@Composable
fun rememberIdentity(vm: AppModel, key: String) =
    rememberSetting<String?>(vm, key, null, { it.takeIf(String::isNotEmpty) }, { it.orEmpty() })

@OptIn(FlowPreview::class)
@Composable
fun rememberDurableScroll(vm: AppModel, key: String): ScrollState {
    val scroll =
        androidx.compose.runtime.key(key) {
            rememberScrollState(vm.prefs.value[key]?.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        }
    LaunchedEffect(scroll, key) {
        snapshotFlow { scroll.value }
            .distinctUntilChanged()
            .debounce(250)
            .collect { vm.preference(key, it.toString()) }
    }
    DisposableEffect(scroll, key) { onDispose { vm.preference(key, scroll.value.toString()) } }
    return scroll
}

@OptIn(FlowPreview::class)
@Composable
fun rememberDurableList(vm: AppModel, key: String): LazyListState {
    val preferences by vm.prefs.collectAsState()
    val saved =
        preferences[key].orEmpty().split(':').map { it.toIntOrNull()?.coerceAtLeast(0) ?: 0 }
    val list =
        androidx.compose.runtime.key(key) {
            rememberLazyListState(saved.getOrElse(0) { 0 }, saved.getOrElse(1) { 0 })
        }
    LaunchedEffect(list, key) {
        snapshotFlow { "${list.firstVisibleItemIndex}:${list.firstVisibleItemScrollOffset}" }
            .distinctUntilChanged()
            .debounce(250)
            .collect { vm.preference(key, it) }
    }
    DisposableEffect(list, key) {
        onDispose {
            vm.preference(key, "${list.firstVisibleItemIndex}:${list.firstVisibleItemScrollOffset}")
        }
    }
    return list
}
