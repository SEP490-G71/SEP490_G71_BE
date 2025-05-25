package com.devintel.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devintel.identityservice.entity.InvalidatedToken;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {}
