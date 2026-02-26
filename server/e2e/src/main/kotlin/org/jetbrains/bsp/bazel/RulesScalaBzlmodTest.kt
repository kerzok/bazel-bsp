package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.minutes

object RulesScalaBzlmodTest : BazelBspTestBaseScenario() {
  private val testClient = createBazelClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    Install.main(
      arrayOf(
        "-d",
        workspaceDir,
        "-b",
        bazelBinary,
        "-t",
        "@//...",
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    error("not needed")
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      testClient.test(5.minutes) { session, _ ->
        val targetsResult = session.server.workspaceBuildTargets().await()

        targetsResult.targets.size shouldBe 2
        targetsResult.targets.map { Label.parse(it.id.uri) } shouldContainExactlyInAnyOrder
          listOf(
            Label.parse("@//:lib"),
            Label.parse("@//:bin"),
          )

        val sourcesResult =
          session.server
            .buildTargetSources(
              SourcesParams(targetsResult.targets.map { it.id }),
            ).await()

        sourcesResult.items.size shouldBe 2

        sourcesResult.items
          .flatMap {
            it.sources
          }.map { Path(it.uri.removePrefix("file:")).relativeTo(Path(workspaceDir)).toString() } shouldContainExactlyInAnyOrder
          listOf(
            "Main.scala",
          )
      }
    }
}
