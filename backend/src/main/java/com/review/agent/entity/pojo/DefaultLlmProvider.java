package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "default_llm_provider", schema = "review_agent")
public class DefaultLlmProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;
    @Size(max = 20)
    @Column(name = "name", length = 20)
    private String name;

    @Size(max = 255)
    @Column(name = "url")
    private String url;

}