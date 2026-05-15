package com.nhuhuy05.enghub.repository;

import com.nhuhuy05.enghub.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    Set<Permission> findByNameIn(Set<String> names);
    void deleteByName(String name);
}
