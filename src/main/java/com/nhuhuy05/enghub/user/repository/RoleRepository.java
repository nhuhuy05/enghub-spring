package com.nhuhuy05.enghub.user.repository;

import com.nhuhuy05.enghub.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Set<Role> findByNameIn(Set<String> names);
    void deleteByName(String name);
}



