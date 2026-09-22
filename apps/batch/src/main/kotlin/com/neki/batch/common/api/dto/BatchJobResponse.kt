package com.neki.batch.common.api.dto

data class BatchJobResponse(val jobName: String, val executionId: Long?, val status: String, val exitCode: String)
