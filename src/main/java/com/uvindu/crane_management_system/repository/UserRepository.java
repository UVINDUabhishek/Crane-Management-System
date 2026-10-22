package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
            select new com.uvindu.crane_management_system.dto.LookupResponse(u.id, u.username)
            from User u where u.active = true
            order by u.username asc, u.id asc
            """)
    List<LookupResponse> findActiveLookupOptions();

    @Query("""
            select new com.uvindu.crane_management_system.dto.LookupResponse(u.id, u.username)
            from User u where u.active = true and u.role in :roles
            order by u.username asc, u.id asc
            """)
    List<LookupResponse> findActiveLookupOptionsByRoles(@Param("roles") List<UserRole> roles);
}
