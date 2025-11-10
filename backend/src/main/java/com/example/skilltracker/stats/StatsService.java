package com.example.skilltracker.stats;

import com.example.skilltracker.skill.SkillEntity;
import com.example.skilltracker.skill.SkillRepository;
import com.example.skilltracker.session.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final SessionRepository sessionRepository;
    private final SkillRepository skillRepository;

    public StatsService(SessionRepository sessionRepository, SkillRepository skillRepository) {
        this.sessionRepository = sessionRepository;
        this.skillRepository = skillRepository;
    }

    public List<SkillStatsDto> aggregateSkillStats(Long skillId, Instant from, Instant to) {
        List<Object[]> rawResults = sessionRepository.aggregateBySkill(skillId, from, to);
        Map<Long, SkillEntity> skillMap = skillRepository.findAll().stream()
                .collect(Collectors.toMap(SkillEntity::getId, skill -> skill));

        return rawResults.stream()
                .map(row -> {
                    Long id = (Long) row[0];
                    SkillEntity skill = skillMap.get(id);
                    String name = skill != null ? skill.getName() : ("Skill #" + id);
                    Long count = (Long) row[1];
                    Long totalDuration = row[2] != null ? (Long) row[2] : 0L;
                    return new SkillStatsDto(id, name, count != null ? count : 0L, totalDuration);
                })
                .sorted(Comparator.comparing(SkillStatsDto::sessionCount).reversed())
                .toList();
    }
}
