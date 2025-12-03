package com.review.agent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tag_relation", schema = "review_agent")
public class TagRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户id
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "main_tag_id")
    private Long mainTagId;

    @Column(name = "sub_tag_id")
    private Long subTagId;

}