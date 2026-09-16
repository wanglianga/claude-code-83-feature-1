package com.community.haircut.repository;

import com.community.haircut.entity.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {

    List<CommunityEvent> findAllByOrderByEventDateDesc();
}
