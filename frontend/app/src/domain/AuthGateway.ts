export interface AuthGateway {
  /**
   * Performs user authentication and returns an access token when successful.
   * @param credentials Login payload such as username/password or social token.
   */
  authenticate(credentials: Record<string, unknown>): Promise<{ token: string }>;

  /**
   * Clears authentication state and revokes associated tokens.
   */
  logout(): Promise<void>;

  /**
   * Restores the currently authenticated session if available.
   */
  restoreSession(): Promise<{ token: string } | null>;
}
