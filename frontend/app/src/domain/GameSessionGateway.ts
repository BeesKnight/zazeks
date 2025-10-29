export interface GameSessionGateway {
  /**
   * Starts a new game session for the provided participant(s).
   * @param params Identifiers and configuration required to create the session.
   */
  startNewSession(params: { playerIds: string[]; seed?: string }): Promise<string>;

  /**
   * Submits a player move to the active session.
   * @param params Contains session identifier and the move payload.
   */
  submitMove(params: { sessionId: string; move: unknown }): Promise<void>;

  /**
   * Subscribes to state updates for the given session.
   * @param sessionId Identifier of the session to observe.
   * @param listener Callback invoked for each state change.
   * @returns A disposer that detaches the listener when called.
   */
  observeSession(
    sessionId: string,
    listener: (state: unknown) => void
  ): () => void;
}
