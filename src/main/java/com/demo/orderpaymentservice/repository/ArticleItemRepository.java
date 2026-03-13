package com.demo.orderpaymentservice.repository;

import com.demo.orderpaymentservice.domain.ArticleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleItemRepository extends JpaRepository<ArticleItem, Long> {

    Optional<ArticleItem> findByIdAndActiveTrue(Long id);
}