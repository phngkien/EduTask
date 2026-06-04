package com.edutask.repository;

import com.edutask.entity.Token;
import com.edutask.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByRefreshToken(String refreshToken);
    void deleteByUser(User user);
    void deleteByUserUserId(Long userId);
}
