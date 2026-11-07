package com.shop.user.repository;

import com.shop.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Written by hand on purpose: derived "findByEmailIgnoreCase" generates upper(email)=upper(?),
     * which would NOT use our functional index ux_users_email_lower ON users (lower(email)).
     */
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);
}
