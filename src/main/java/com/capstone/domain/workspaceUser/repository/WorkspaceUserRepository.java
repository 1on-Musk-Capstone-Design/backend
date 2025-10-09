package com.capstone.domain.workspaceUser.repository;

import com.capstone.domain.workspaceUser.entity.WorkspaceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceUserRepository extends JpaRepository<WorkspaceUser, Long> {

}
