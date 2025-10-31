package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerServiceTest {
    private InMemoryDatabase database;
    private MultiplayerService multiplayerService;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        database = new InMemoryDatabase();
        multiplayerService = new MultiplayerService(database);
        alice = new User("alice", "hash", null);
        bob = new User("bob", "hash", null);
        database.saveUser(alice);
        database.saveUser(bob);
    }

    @Test
    void playersAreMatchedAndResultPersisted() {
        MultiplayerService.MatchState waiting = multiplayerService.joinQueue(alice.getId());
        assertEquals(MultiplayerService.MatchStatus.WAITING_FOR_OPPONENT, waiting.status());

        MultiplayerService.MatchState ready = multiplayerService.joinQueue(bob.getId());
        assertEquals(MultiplayerService.MatchStatus.READY, ready.status());
        assertNotEquals(waiting.id(), ready.id());

        MultiplayerService.MatchState afterAlice = multiplayerService.submitGesture(alice.getId(), ready.id(), "rock");
        assertEquals(MultiplayerService.MatchStatus.READY, afterAlice.status());
        assertEquals("rock", afterAlice.playerOneGesture());

        MultiplayerService.MatchState complete = multiplayerService.submitGesture(bob.getId(), ready.id(), "scissors");
        assertEquals(MultiplayerService.MatchStatus.COMPLETED, complete.status());
        assertEquals("player1", complete.winner());
        assertEquals(1, database.findAllMultiplayerGames().size());
        assertEquals(1, database.findUserById(alice.getId()).orElseThrow().getOnlineWins());
        assertEquals(1, database.findUserById(alice.getId()).orElseThrow().getOnlineGames());
        assertEquals(1, database.findUserById(bob.getId()).orElseThrow().getOnlineGames());
    }

    @Test
    void invalidGestureRejected() {
        multiplayerService.joinQueue(alice.getId());
        MultiplayerService.MatchState match = multiplayerService.joinQueue(bob.getId());
        assertThrows(IllegalArgumentException.class, () -> multiplayerService.submitGesture(alice.getId(), match.id(), "lizard"));
    }
}
