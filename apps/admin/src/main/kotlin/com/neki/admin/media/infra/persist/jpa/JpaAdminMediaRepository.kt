package com.neki.admin.media.infra.persist.jpa

import com.neki.domain.media.models.Media
import org.springframework.data.jpa.repository.JpaRepository

interface JpaAdminMediaRepository : JpaRepository<Media, Long>
