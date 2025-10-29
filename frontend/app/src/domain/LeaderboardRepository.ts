export interface LeaderboardRepository {
  /**
   * Retrieves leaderboard entries sorted according to the specified strategy.
   * @param params Filters and pagination details.
   */
  getLeaderboard(params?: { limit?: number; offset?: number; scope?: string }): Promise<unknown[]>;

  /**
   * Forces a refresh of cached leaderboard data.
   */
  refreshLeaderboard(scope?: string): Promise<void>;
}
