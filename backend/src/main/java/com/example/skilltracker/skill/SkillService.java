package com.example.skilltracker.skill;

import com.example.skilltracker.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public SkillDto createSkill(CreateSkillRequest request) {
        SkillEntity entity = new SkillEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        entity.setColor(request.color());
        entity.setArchived(false);
        SkillEntity saved = skillRepository.save(entity);
        return SkillMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SkillDto> getAllSkills(Boolean includeArchived) {
        List<SkillEntity> skills;
        if (Boolean.TRUE.equals(includeArchived)) {
            skills = skillRepository.findAll();
        } else {
            skills = skillRepository.findByArchived(false);
        }
        return skills.stream()
                .map(SkillMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillDto getSkill(Long id) {
        return SkillMapper.toDto(findSkill(id));
    }

    public SkillDto updateSkill(Long id, UpdateSkillRequest request) {
        SkillEntity entity = findSkill(id);
        SkillMapper.updateEntity(entity, request);
        return SkillMapper.toDto(skillRepository.save(entity));
    }

    public SkillDto archiveSkill(Long id) {
        SkillEntity entity = findSkill(id);
        entity.setArchived(true);
        return SkillMapper.toDto(skillRepository.save(entity));
    }

    public void deleteSkill(Long id) {
        SkillEntity entity = findSkill(id);
        skillRepository.delete(entity);
    }

    public SkillEntity findSkill(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill %d not found".formatted(id)));
    }
}
