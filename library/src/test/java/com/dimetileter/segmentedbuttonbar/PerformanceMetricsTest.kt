package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlin.system.measureNanoTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test-only performance metric logging. Output is written under build/, which is git-ignored.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceMetricsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    @Test
    fun logsPerformanceMetricsUnderIgnoredBuildDirectory() {
        val metricsFile = metricsFile()
        metricsFile.parentFile?.mkdirs()

        val initNanos = measureNanoTime {
            repeat(50) {
                SegmentedButtonBar(context)
            }
        }

        val selectionAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()
        val selectionBar = SegmentedButtonBar(context, selectionAttrs)
        val selectionNanos = measureNanoTime {
            repeat(300) { index ->
                selectionBar.selectButton(index % selectionBar.getButtonCount(), animate = false)
            }
        }

        val expandableAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()
        val expandableBar = SegmentedButtonBar(context, expandableAttrs)
        val expandCollapseNanos = measureNanoTime {
            repeat(100) {
                expandableBar.expand(animate = false)
                expandableBar.collapse(animate = false)
            }
        }

        metricsFile.appendText(
            buildString {
                append("timestamp=").append(Instant.now())
                append(", init_50_ns=").append(initNanos)
                append(", select_300_ns=").append(selectionNanos)
                append(", expand_collapse_100_ns=").append(expandCollapseNanos)
                append('\n')
            }
        )

        assertThat(metricsFile.exists()).isTrue()
        assertThat(metricsFile.readText()).contains("expand_collapse_100_ns")
    }

    private fun metricsFile(): File {
        val currentDir = File(requireNotNull(System.getProperty("user.dir")))
        val moduleDir = if (File(currentDir, "src/main").exists()) {
            currentDir
        } else {
            File(currentDir, "library")
        }
        return File(moduleDir, "build/performance-metrics/segmented-button-bar-metrics.log")
    }
}
