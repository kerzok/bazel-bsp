package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.time.Duration.Companion.minutes

object NestedModulesTest : BazelBspTestBaseScenario() {
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
        "-t",
        "@inner//...",
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      compareWorkspaceRepoMappingResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    error("not needed")
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      testClient.test(3.minutes) { session, _ ->
        val targetsResult = session.server.workspaceBuildTargets().await()

        val sep = bzlmodRepoNameSeparator
        targetsResult.targets.size shouldBe 4
        targetsResult.targets.map { Label.parse(it.id.uri) } shouldContainExactlyInAnyOrder
          listOf(
            Label.parse("@@inner$sep//:lib_inner"),
            Label.parse("@@inner$sep//:bin_inner"),
            Label.parse("@//:lib_outer"),
            Label.parse("@//:bin_outer"),
          )

        val sourcesResult =
          session.server
            .buildTargetSources(
              SourcesParams(targetsResult.targets.map { it.id }),
            ).await()

        sourcesResult.items.size shouldBe 4

        sourcesResult.items
          .flatMap {
            it.sources
          }.map { Path(it.uri.removePrefix("file:")).relativeTo(Path(workspaceDir)).toString() } shouldContainExactlyInAnyOrder
          listOf(
            "BinOuter.java",
            "LibOuter.java",
            "inner/BinInner.java",
            "inner/LibInner.java",
          )
      }
    }

  private fun compareWorkspaceRepoMappingResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace repo mapping results",
    ) {
      testClient.test(3.minutes) { session, _ ->
        val repoMapping = session.server.workspaceBazelRepoMapping().await()

        val sep = bzlmodRepoNameSeparator
        val expectedApparentMapping =
          mutableMapOf(
            "" to "",
            "local_config_platform" to "local_config_platform",
            "rules_java" to "rules_java$sep",
            "rules_python" to "rules_python$sep",
            "bazel_tools" to "bazel_tools",
            "outer" to "",
            "inner" to "inner$sep",
          )
        if (majorBazelVersion >= 8) {
          expectedApparentMapping["bazelbsp_aspect"] = "${sep}_repo_rules${sep}bazelbsp_aspect"
        }
        repoMapping.apparentRepoNameToCanonicalName shouldContainAll expectedApparentMapping

        val canonicalMapping = repoMapping.canonicalRepoNameToPath
        canonicalMapping.keys.containsAll(repoMapping.apparentRepoNameToCanonicalName.values) shouldBe true
        canonicalMapping[""] shouldBe "file://$workspaceDir/"
        if (majorBazelVersion >= 8) {
          canonicalMapping
            .getValue(
              "${sep}_repo_rules${sep}bazelbsp_aspect",
            ).shouldEndWith("/external/${sep}_repo_rules${sep}bazelbsp_aspect/")
        }
        canonicalMapping.getValue("local_config_platform").shouldEndWith("/external/local_config_platform/")
        canonicalMapping.getValue("rules_java$sep").shouldEndWith("/external/rules_java$sep/")
        canonicalMapping["inner$sep"] shouldBe "file://$workspaceDir/inner/"
        canonicalMapping["bazel_tools"] shouldEndWith ("/external/bazel_tools/")

        for (canonicalName in expectedApparentMapping.values.toSet()) {
          val path = canonicalMapping.getValue(canonicalName)
          URI.create(path).toPath().shouldExist()
        }
      }
    }
}
