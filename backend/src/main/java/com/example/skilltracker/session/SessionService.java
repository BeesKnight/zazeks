package com.example.skilltracker.session;

import com.example.skilltracker.common.ResourceNotFoundException;
import com.example.skilltracker.skill.SkillEntity;
import com.example.skilltracker.skill.SkillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        entity.setSessionDate(request.sessionDate());
        entity.setDurationMinutes(request.durationMinutes());
        entity.setNotes(request.notes());
        SessionEntity saved = sessionRepository.save(entity);
        return SessionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> findBySkill(Long skillId) {
        SkillEntity skill = skillService.findSkill(skillId);
        return sessionRepository.findBySkill(skill).stream()
                .map(SessionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionDto getSession(Long id) {
        return SessionMapper.toDto(sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session %d not found".formatted(id))));
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getAll() {
        return sessionRepository.findAll().stream()
                .map(SessionMapper::toDto)
                .toList();
    }
}
