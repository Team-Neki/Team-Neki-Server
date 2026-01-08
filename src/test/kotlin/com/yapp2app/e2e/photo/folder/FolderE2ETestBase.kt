package com.yapp2app.e2e.photo.folder

import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : FolderE2ETestBase
 * author         : koo
 * date           : 2025. 12. 29. 오전 2:43
 * description    :
 */
abstract class FolderE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var folderRepository: JpaFolderRepository

    @AfterEach
    override fun tearDown() {
        folderRepository.deleteAllInBatch()
        super.tearDown()
    }
}
