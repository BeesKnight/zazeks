export interface PlayerProfileRepository {
  /**
   * Loads the profile for a specific player.
   * @param playerId Unique identifier of the player whose profile should be fetched.
   */
  getProfile(playerId: string): Promise<unknown>;

  /**
   * Persists profile changes such as nickname or avatar.
   * @param playerId Identifier of the player being updated.
   * @param payload Key-value pairs representing profile fields to be stored.
   */
  updateProfile(playerId: string, payload: Record<string, unknown>): Promise<void>;
}
