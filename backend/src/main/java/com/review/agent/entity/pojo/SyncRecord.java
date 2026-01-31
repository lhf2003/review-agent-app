package com.review.agent.entity.pojo;

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

    /**
     * 同步状态：0=成功, 1=同步中, 2=失败
     */
    @ColumnDefault("0")
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 同步消息或错误描述
     */
    @Column(name = "message", length = 500)
    private String message;

}