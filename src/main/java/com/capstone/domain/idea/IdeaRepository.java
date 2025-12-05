package com.capstone.domain.idea;

import com.capstone.domain.workspace.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdeaRepository extends JpaRepository<Idea, Long> {
  List<Idea> findByWorkspace(Workspace workspace);
}
