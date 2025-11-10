package com.example.skilltracker.session;

import com.example.skilltracker.common.ResourceNotFoundException;
import com.example.skilltracker.skill.SkillEntity;
import com.example.skilltracker.skill.SkillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SkillService skillService;

    public SessionService(SessionRepository sessionRepository, SkillService skillService) {
        this.sessionRepository = sessionRepository;
        this.skillService = skillService;
    }

    public SessionDto createSession(CreateSessionRequest request) {
        SkillEntity skill = skillService.findSkill(request.skillId());
        SessionEntity entity = new SessionEntity();
        entity.setSkill(skill);
        entity.setSessionDate(resolveSessionDate(request.sessionDate()));
        entity.setDurationMinutes(request.durationMinutes());
        entity.setNotes(request.notes());
        entity.setDifficulty(request.difficulty());
        entity.setSource(request.source());
        SessionEntity saved = sessionRepository.save(entity);
        return SessionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getSessions(Long skillId, Instant from, Instant to) {
        if (skillId != null) {
            skillService.findSkill(skillId);
        }
        return sessionRepository.findByFilters(skillId, from, to).stream()
                .map(SessionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionDto> findBySkill(Long skillId) {
        return getSessions(skillId, null, null);
    }

    @Transactional(readOnly = true)
    public SessionDto getSession(Long id) {
        return SessionMapper.toDto(findSession(id));
    }

    public SessionDto updateSession(Long id, UpdateSessionRequest request) {
        SessionEntity entity = findSession(id);
        SkillEntity skill = skillService.findSkill(request.skillId());
        entity.setSkill(skill);
        entity.setSessionDate(resolveSessionDate(request.sessionDate()));
        entity.setDurationMinutes(request.durationMinutes());
        entity.setNotes(request.notes());
        entity.setDifficulty(request.difficulty());
        entity.setSource(request.source());
        SessionEntity saved = sessionRepository.save(entity);
        return SessionMapper.toDto(saved);
    }

    public void deleteSession(Long id) {
        SessionEntity entity = findSession(id);
        sessionRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getAll() {
        return getSessions(null, null, null);
    }

    private SessionEntity findSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session %d not found".formatted(id)));
    }

    private Instant resolveSessionDate(Instant provided) {
        return provided != null ? provided : Instant.now();
    }
}
