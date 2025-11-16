package com.example.minitasker.repository;

import com.example.minitasker.model.Project;
import com.example.minitasker.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByOwner(User owner, Pageable pageable);
    Page<Project> findByOwnerAndNameContainingIgnoreCase(User owner, String name, Pageable pageable);
    Page<Project> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Project> findByOwner(User owner);
    Optional<Project> findByIdAndOwner(Long id, User owner);
}
