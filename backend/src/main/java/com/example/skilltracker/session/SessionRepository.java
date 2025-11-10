package com.example.skilltracker.session;

import com.example.skilltracker.skill.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    List<SessionEntity> findBySkill(SkillEntity skill);

    @Query("SELECT s FROM SessionEntity s WHERE (:skillId IS NULL OR s.skill.id = :skillId) AND (:fromDate IS NULL OR s.sessionDate >= :fromDate) AND (:toDate IS NULL OR s.sessionDate <= :toDate)")
    List<SessionEntity> findByFilters(@Param("skillId") Long skillId,
                                      @Param("fromDate") Instant fromDate,
                                      @Param("toDate") Instant toDate);

    @Query("SELECT s.skill.id, COUNT(s), SUM(s.durationMinutes) FROM SessionEntity s WHERE (:skillId IS NULL OR s.skill.id = :skillId) AND (:fromDate IS NULL OR s.sessionDate >= :fromDate) AND (:toDate IS NULL OR s.sessionDate <= :toDate) GROUP BY s.skill.id")
    List<Object[]> aggregateBySkill(@Param("skillId") Long skillId,
                                    @Param("fromDate") Instant fromDate,
                                    @Param("toDate") Instant toDate);

    @Query("SELECT s.skill.id, MAX(s.sessionDate) FROM SessionEntity s GROUP BY s.skill.id")
    List<Object[]> findLastSessionDates();
}
