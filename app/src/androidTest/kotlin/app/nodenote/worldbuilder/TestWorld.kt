package app.nodenote.worldbuilder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule

fun createSampleForTest(
    rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
): AppModel {
    lateinit var vm: AppModel
    rule.runOnIdle { vm = ViewModelProvider(rule.activity)[AppModel::class.java] }
    rule.waitUntil(20000) { vm.prefsReady.value }
    val old = vm.bundle.value?.world?.id
    rule.runOnIdle { vm.newWorld("UI fixture", true) }
    rule.waitUntil(30000) {
        vm.bundle.value?.world?.id != null && vm.bundle.value?.world?.id != old
    }
    rule.waitUntil(20000) {
        rule.onAllNodesWithText("Lore library").fetchSemanticsNodes().isNotEmpty()
    }
    return vm
}
