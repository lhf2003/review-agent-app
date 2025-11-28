package com.review.agent.entity;

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
@Table(name = "file_info", schema = "review_agent")
public class FileInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ColumnDefault("1")
    @Column(name = "user_id")
    private Integer userId;

    @Size(max = 255)
    @NotNull
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Size(max = 100)
    @NotNull
    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    @Lob
    @Column(name = "file_content")
    private String fileContent;

    /**
     * 处理状态（0=未分析, 1=已分析 2=正在分析）
     */
    @ColumnDefault("0")
    @Column(name = "processed_status")
    private Integer processedStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_time")
    private Date createdTime;

}