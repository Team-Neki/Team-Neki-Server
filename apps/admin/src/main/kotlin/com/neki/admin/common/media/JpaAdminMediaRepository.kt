package com.neki.admin.common.media

import com.neki.domain.media.models.Media
import org.springframework.data.jpa.repository.JpaRepository

interface JpaAdminMediaRepository : JpaRepository<Media, Long>
