package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "data_info", schema = "review_agent")
public class DataInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Size(max = 100)
    @NotNull
    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    @Lob
    @Column(name = "file_content")
    private String fileContent;

    /**
     * 数据来源 (0=LOCAL, 1=GEMINI, 2=CHATGPT)
     */
    @ColumnDefault("0")
    @Column(name = "source")
    private Integer source;

    /**
     * 处理状态（0=未分析, 1=已分析 2=正在分析）
     */
    @ColumnDefault("0")
    @Column(name = "processed_status")
    private Integer processedStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "created_time")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 删除标记（软删除）
     * false-未删除，true-已删除
     */
    @ColumnDefault("0")
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * 删除时间（软删除）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "deleted_at")
    private Date deletedAt;

}