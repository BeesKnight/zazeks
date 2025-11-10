package com.example.skilltracker.session;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto create(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.createSession(request);
    }

    @GetMapping
    public List<SessionDto> getAll() {
        return sessionService.getAll();
    }

    @GetMapping("/{id}")
    public SessionDto get(@PathVariable Long id) {
        return sessionService.getSession(id);
    }

    @GetMapping("/skill/{skillId}")
    public List<SessionDto> getBySkill(@PathVariable Long skillId) {
        return sessionService.findBySkill(skillId);
    }
}
