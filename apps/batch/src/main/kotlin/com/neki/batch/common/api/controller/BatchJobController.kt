package com.neki.batch.common.api.controller

import com.neki.batch.common.api.dto.BatchJobResponse
import com.neki.batch.common.job.BatchJobLauncher
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import org.springframework.batch.core.JobExecution
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 잡 수동 트리거 API. cron 을 기다리지 않고 잡 기동을 검증한다.
 *
 * 인증이 없으므로 `neki.batch.trigger-api-enabled=true` 인 pod 에서만 빈이 등록되며,
 * k8s Service/Ingress 로 외부에 노출하지 않는다 (kubectl port-forward 로 호출).
 * JobLauncher 가 동기 실행이라 잡이 끝날 때까지 응답이 블로킹된다.
 */
@RestController
@RequestMapping("/batch/jobs")
@ConditionalOnProperty(prefix = "neki.batch", name = ["trigger-api-enabled"], havingValue = "true")
class BatchJobController(private val launcher: BatchJobLauncher) {

    @PostMapping("/{jobName}")
    fun trigger(
        @PathVariable jobName: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) businessDate: LocalDate?,
    ): ResponseEntity<BaseResponse<BatchJobResponse>> {
        if (jobName !in launcher.availableJobNames) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BaseResponse(ResultCode.NOT_FOUND.code, "알 수 없는 잡: $jobName (가능: ${launcher.availableJobNames})"),
            )
        }

        val execution: JobExecution = launcher.launchByName(jobName, businessDate)
            ?: return ResponseEntity.status(HttpStatus.CONFLICT).body(
                BaseResponse(ResultCode.ALREADY_REQUEST.code, "이미 실행 중인 잡: $jobName"),
            )

        return ResponseEntity.ok(
            BaseResponse(
                data = BatchJobResponse(
                    jobName = jobName,
                    executionId = execution.id,
                    status = execution.status.name,
                    exitCode = execution.exitStatus.exitCode,
                ),
            ),
        )
    }
}
