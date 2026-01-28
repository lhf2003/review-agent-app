package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_llm_config", schema = "review_agent")
public class UserLlmConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Size(max = 20)
    @Column(name = "name", length = 20)
    private String name;

    @Size(max = 255)
    @Column(name = "url")
    private String url;

    @Size(max = 255)
    @Column
    private String apiKey;

    @JsonProperty("isConnected")
    @Column(name = "is_connected")
    private boolean isConnected;
}