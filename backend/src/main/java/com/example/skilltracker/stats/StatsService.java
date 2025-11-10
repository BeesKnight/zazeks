package com.example.skilltracker.stats;

import com.example.skilltracker.common.ResourceNotFoundException;
import com.example.skilltracker.session.SessionEntity;
import com.example.skilltracker.session.SessionRepository;
import com.example.skilltracker.skill.SkillEntity;
import com.example.skilltracker.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final long INACTIVE_THRESHOLD_DAYS = 30;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

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

    public OverviewStatsDto getOverviewStats(Instant from, Instant to) {
        Instant toDate = resolveTo(to);
        Instant fromDate = resolveFrom(from, toDate);

        List<SessionEntity> sessions = sessionRepository.findByFilters(null, fromDate, toDate);
        long totalMinutes = sessions.stream().mapToLong(SessionEntity::getDurationMinutes).sum();
        long sessionsCount = sessions.size();

        List<OverviewSkillBreakdownDto> bySkill = sessions.stream()
                .collect(Collectors.groupingBy(session -> session.getSkill().getId()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<SessionEntity> skillSessions = entry.getValue();
                    SkillEntity skill = skillSessions.get(0).getSkill();
                    long minutes = skillSessions.stream().mapToLong(SessionEntity::getDurationMinutes).sum();
                    long count = skillSessions.size();
                    return new OverviewSkillBreakdownDto(skill.getId(), skill.getName(), minutes, count);
                })
                .sorted(Comparator.comparingLong(OverviewSkillBreakdownDto::minutes).reversed())
                .toList();

        List<SkillEntity> skills = skillRepository.findAll();
        Map<Long, Instant> lastSessions = sessionRepository.findLastSessionDates().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Instant) row[1]));
        Instant now = Instant.now();

        List<InactiveSkillDto> inactiveSkills = skills.stream()
                .filter(skill -> !skill.isArchived())
                .map(skill -> {
                    Instant last = lastSessions.get(skill.getId());
                    Instant reference = last != null ? last : skill.getCreatedAt();
                    long daysSince = reference != null ? ChronoUnit.DAYS.between(reference, now) : INACTIVE_THRESHOLD_DAYS + 1;
                    return new InactiveSkillDto(skill.getId(), skill.getName(), daysSince);
                })
                .filter(dto -> dto.daysSinceLastSession() > INACTIVE_THRESHOLD_DAYS)
                .sorted(Comparator.comparingLong(InactiveSkillDto::daysSinceLastSession).reversed())
                .toList();

        long skillsCount = skillRepository.count();
        return new OverviewStatsDto(totalMinutes, skillsCount, sessionsCount, bySkill, inactiveSkills);
    }

    public SkillDetailsStatsDto getSkillDetails(Long skillId, Instant from, Instant to) {
        SkillEntity skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill %d not found".formatted(skillId)));
        Instant toDate = resolveTo(to);
        Instant fromDate = resolveFrom(from, toDate);

        List<SessionEntity> sessions = sessionRepository.findByFilters(skillId, fromDate, toDate);
        long totalMinutes = sessions.stream().mapToLong(SessionEntity::getDurationMinutes).sum();
        long sessionsCount = sessions.size();
        OptionalDouble averageDifficulty = sessions.stream()
                .map(SessionEntity::getDifficulty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();

        List<SkillDailyStatsDto> byDay = sessions.stream()
                .collect(Collectors.groupingBy(session -> session.getSessionDate().atZone(DEFAULT_ZONE).toLocalDate(),
                        Collectors.summingLong(SessionEntity::getDurationMinutes)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SkillDailyStatsDto(entry.getKey(), entry.getValue()))
                .toList();

        Double average = averageDifficulty.isPresent() ? averageDifficulty.getAsDouble() : null;
        return new SkillDetailsStatsDto(skill.getId(), skill.getName(), totalMinutes, sessionsCount, average, byDay);
    }

    private Instant resolveTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private Instant resolveFrom(Instant from, Instant to) {
        if (from != null) {
            return from;
        }
        return to.minus(30, ChronoUnit.DAYS);
    }
}
