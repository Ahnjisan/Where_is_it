package com.whereisit.backend.founditem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {

	Optional<FoundItem> findBySourceTypeAndAtcIdAndFdSn(FoundItemSourceType sourceType, String atcId, String fdSn);
}
