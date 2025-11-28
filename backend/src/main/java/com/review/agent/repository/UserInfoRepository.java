package com.review.agent.repository;

import com.review.agent.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
  @Query("select u from UserInfo u where u.username = ?1")
  UserInfo findByUsername(String username);
}