package com.smartagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.hypersistence.utils.hibernate.id.Tsid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Base entity with common fields for all domain objects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@MappedSuperclass
public abstract class BaseEntity {

    @Tsid
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @TableLogic
    @Column(name = "deleted")
    private Boolean deleted = false;
}
