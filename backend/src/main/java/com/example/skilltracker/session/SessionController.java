package com.example.skilltracker.session;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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
    public List<SessionDto> getAll(@RequestParam(required = false) Long skillId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                   Instant from,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                   Instant to) {
        return sessionService.getSessions(skillId, from, to);
    }

    @GetMapping("/{id}")
    public SessionDto get(@PathVariable Long id) {
        return sessionService.getSession(id);
    }

    @PutMapping("/{id}")
    public SessionDto update(@PathVariable Long id, @Valid @RequestBody UpdateSessionRequest request) {
        return sessionService.updateSession(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sessionService.deleteSession(id);
    }

    @GetMapping("/skill/{skillId}")
    public List<SessionDto> getBySkill(@PathVariable Long skillId) {
        return sessionService.findBySkill(skillId);
    }
}
