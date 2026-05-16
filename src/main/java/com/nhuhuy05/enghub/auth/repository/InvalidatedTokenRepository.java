package com.nhuhuy05.enghub.auth.repository;

import com.nhuhuy05.enghub.auth.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
}

