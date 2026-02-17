package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import kotlin.time.Duration.Companion.minutes

object BazelBspTypeScriptProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  override fun additionalServerInstallArguments(): Array<String> =
    arrayOf(
      "--enabled-rules",
      "aspect_rules_jest",
      "--enabled-rules",
      "aspect_rules_ts",
      "--enabled-rules",
      "rules_ts",
      "--enabled-rules",
      "rules_jest",
    )

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult =
    WorkspaceBuildTargetsResult(
      listOf(
        helloTestBuildTarget(),
        exampleLibTestBuildTarget(),
        appWebTestBuildTarget(),
      ),
    )

  private fun helloTestBuildTarget(): BuildTarget {
    val buildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//example:hello_test"),
        listOf("test"),
        listOf("typescript"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    buildTarget.displayName = "$targetPrefix//example:hello_test"
    buildTarget.baseDirectory = "file://\$WORKSPACE/example/"
    return buildTarget
  }

  private fun exampleLibTestBuildTarget(): BuildTarget {
    val buildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//lib:example_lib_test"),
        listOf("test"),
        listOf("typescript"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    buildTarget.displayName = "$targetPrefix//lib:example_lib_test"
    buildTarget.baseDirectory = "file://\$WORKSPACE/lib/"
    return buildTarget
  }

  private fun appWebTestBuildTarget(): BuildTarget {
    val buildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//webtest:app_test"),
        listOf("test"),
        listOf("typescript"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    buildTarget.displayName = "$targetPrefix//webtest:app_test"
    buildTarget.baseDirectory = "file://\$WORKSPACE/webtest/"
    return buildTarget
  }

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep {
    val workspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        1.minutes,
        workspaceBuildTargetsResult,
      )
    }
  }

  private fun webTestSources(): BazelBspTestScenarioStep {
    val appTsSource =
      SourceItem(
        "file://\$WORKSPACE/webtest/app.ts",
        SourceItemKind.FILE,
        false,
      )
    val appTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//webtest:app_test"),
        listOf(appTsSource),
      )
    appTestSources.roots = emptyList()

    val sourcesParams = SourcesParams(listOf(BuildTargetIdentifier("$targetPrefix//webtest:app_test")))
    val expectedSourcesResult = SourcesResult(listOf(appTestSources))

    return BazelBspTestScenarioStep("web_test sources") {
      testClient.testSources(
        1.minutes,
        sourcesParams,
        expectedSourcesResult,
      )
    }
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
      webTestSources(),
    )
}
