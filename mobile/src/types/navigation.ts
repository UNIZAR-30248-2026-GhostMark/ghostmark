export type RootStackParamList = {
  Welcome: undefined;
  SignIn: undefined;
  Schedule: { onboarding?: boolean } | undefined;
  LocationConsent: undefined;
  AvailabilityConsent: undefined;
  Profile: undefined;
  Home: { createdGame?: import("./game").GameSummary } | undefined;
  CreateGameData: undefined;
  CreateGameRules: { game: import("./game").GameDraft; startLabel: string; endLabel: string };
  ApiStatus: undefined;
};
