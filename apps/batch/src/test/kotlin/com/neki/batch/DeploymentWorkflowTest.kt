package com.neki.batch

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

/** 선택 ref를 실행하는 빌드와 GitOps 쓰기 자격증명이 다시 한 runner에 섞이지 않게 검증한다. */
class DeploymentWorkflowTest {

    private val workflow: Map<String, Any> = loadWorkflow()
    private val jobs: Map<String, Map<String, Any>> = map(workflow.getValue("jobs"))

    @Test
    fun `선택 ref 빌드에는 GitOps PAT와 알림 webhook을 제공하지 않는다`() {
        val build: Map<String, Any> = jobs.getValue("build")
        build["needs"] shouldBe "preflight"
        build.toString().contains("GITOPS_PAT") shouldBe false
        build.toString().contains("DISCORD_WEBHOOK") shouldBe false
        steps(build).filter { it.containsKey("uses") && it["uses"].toString().startsWith("actions/checkout@") }
            .forEach { map<String, Any>(it.getValue("with"))["persist-credentials"] shouldBe false }
    }

    @Test
    fun `GitOps 쓰기는 격리된 job에서만 하고 선택 ref 코드를 실행하지 않는다`() {
        val deploy: Map<String, Any> = jobs.getValue("deploy")
        deploy["needs"] shouldBe "build"
        deploy.toString().contains("./gradlew") shouldBe false
        val checkout: Map<String, Any> = steps(deploy).first { it.containsKey("uses") }
        val options: Map<String, Any> = map(checkout.getValue("with"))
        options["repository"] shouldBe "\${{ env.GITOPS_REPO }}"
        options.containsKey("ref") shouldBe false
        options["token"] shouldBe "\${{ secrets.GITOPS_PAT }}"
        options["persist-credentials"] shouldBe true
    }

    @Test
    fun `모든 외부 action은 전체 commit SHA를 참조한다`() {
        jobs.values.flatMap(::steps).filter { it.containsKey("uses") }.forEach {
            Regex("[^@]+@[0-9a-f]{40}").matches(it.getValue("uses").toString()) shouldBe true
        }
    }

    @Test
    fun `사전 검사와 실패 알림은 빌드 성공 여부와 분리한다`() {
        jobs.getValue("preflight").containsKey("needs") shouldBe false
        val checkout: Map<String, Any> = steps(jobs.getValue("preflight")).first { it.containsKey("uses") }
        map<String, Any>(checkout.getValue("with"))["persist-credentials"] shouldBe false
        val notify: Map<String, Any> = jobs.getValue("notify")
        notify["needs"] shouldBe listOf("preflight", "build", "deploy")
        notify["if"] shouldBe "always()"
    }

    private fun loadWorkflow(): Map<String, Any> {
        val relative = Path.of(".github/workflows/deploy-batch.yml")
        val root: Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve(relative)) }
        return Files.newBufferedReader(root.resolve(relative)).use { Yaml().load(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V> map(value: Any): Map<K, V> = value as Map<K, V>

    @Suppress("UNCHECKED_CAST")
    private fun steps(job: Map<String, Any>): List<Map<String, Any>> = job.getValue("steps") as List<Map<String, Any>>
}
