package com.review.agent.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "sync_record", schema = "review_agent")
public class SyncRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "spend_time", nullable = false)
    private Double spendTime;

    @Column(name = "sync_count", nullable = false)
    private Integer syncCount;

    @ColumnDefault("(now())")
    @Column(name = "create_time")
    private Date createTime;

}