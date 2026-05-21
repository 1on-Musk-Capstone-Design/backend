package com.capstone.domain.idea.prototype;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaPrototypeJobRepository extends JpaRepository<IdeaPrototypeJob, Long> {

  Optional<IdeaPrototypeJob> findTopByIdea_IdOrderByIdDesc(Long ideaId);

  List<IdeaPrototypeJob> findByIdea_IdOrderByIdDesc(Long ideaId);

  Optional<IdeaPrototypeJob> findByIdea_IdAndId(Long ideaId, Long jobId);
}
