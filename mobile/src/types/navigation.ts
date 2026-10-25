export type RootStackParamList = {
  Welcome: undefined;
  SignIn: undefined;
  Schedule: { onboarding?: boolean; gameId?: string } | undefined;
  LocationConsent: undefined;
  AvailabilityConsent: undefined;
  Profile: undefined;
  Home: { createdGame?: import("./game").GameSummary } | undefined;
  CreateGameData: undefined;
  CreateGameRules: { game: import("./game").GameDraft; startLabel: string; endLabel: string };
  ShareGame: { game: import("./game").Game };
  GameInvitation: { code: string };
  GameNorms: { code: string; game: import("./game").PublicGame };
  WaitingRoom: { gameId: string; game: import("./game").PublicGame };
  ScanInvitation: undefined;
  ApiStatus: undefined;
};
