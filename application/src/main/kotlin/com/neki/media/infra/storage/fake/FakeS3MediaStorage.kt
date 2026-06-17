package com.neki.media.infra.storage.fake

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * fileName       : FakeS3MediaStorage
 * author         : koo
 * date           : 2025. 12. 28. 오후 10:30
 * description    :
 */
@Profile("test")
@Configuration
class FakeS3MediaStorage
