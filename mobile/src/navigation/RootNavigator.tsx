import { createNavigationContainerRef, DarkTheme, NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Linking, Text, View } from "react-native";

import { getCurrentAccount, nextAccountScreen, restoreSession, setSessionExpiredHandler } from "../api/auth";
import { ApiStatusScreen } from "../screens/ApiStatusScreen";
import { AvailabilityConsentScreen } from "../screens/AvailabilityConsentScreen";
import { CreateGameDataScreen } from "../screens/CreateGameDataScreen";
import { CreateGameRulesScreen } from "../screens/CreateGameRulesScreen";
import { GameInvitationScreen } from "../screens/GameInvitationScreen";
import { GameCancelledScreen } from "../screens/GameCancelledScreen";
import { GameNormsScreen } from "../screens/GameNormsScreen";
import { GameStartedScreen } from "../screens/GameStartedScreen";
import { MissionCardScreen } from "../screens/MissionCardScreen";
import { ShareGameScreen } from "../screens/ShareGameScreen";
import { ScanInvitationScreen } from "../screens/ScanInvitationScreen";
import { WaitingRoomScreen } from "../screens/WaitingRoomScreen";
import { createInvitationInbox } from "../utils/invitation";
import { serverWakeupMessage, useServerWakeupNotice } from "../utils/serverWakeup";
import { HomeScreen } from "../screens/HomeScreen";
import { LocationConsentScreen } from "../screens/LocationConsentScreen";
import { ProfileScreen } from "../screens/ProfileScreen";
import { SignInScreen } from "../screens/SignInScreen";
import { ScheduleScreen } from "../screens/ScheduleScreen";
import { WelcomeScreen } from "../screens/WelcomeScreen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import type { RootStackParamList } from "../types/navigation";

const Stack = createNativeStackNavigator<RootStackParamList>();
const navigationRef = createNavigationContainerRef<RootStackParamList>();

const navigationTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    background: colors.background,
    card: colors.background,
    text: colors.text,
    border: colors.border,
    primary: colors.lime,
  },
};

export function RootNavigator() {
  const [initialRoute, setInitialRoute] = useState<"SignIn" | "Home" | "LocationConsent" | "AvailabilityConsent">();
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const invitations = useRef(createInvitationInbox());
  const showServerWakeupNotice = useServerWakeupNotice(!initialRoute && !error, attempt);

  const openPendingInvitation = useCallback(() => {
    if (!navigationRef.isReady()) return;
    const screen = navigationRef.getCurrentRoute()?.name;
    const ready = Boolean(getCurrentAccount()) && nextAccountScreen() === "Home"
      && screen !== "Welcome" && screen !== "SignIn";
    const code = invitations.current.take(ready);
    if (code) navigationRef.navigate("GameInvitation", { code });
  }, []);

  useEffect(() => {
    let active = true;
    let receivedLiveLink = false;
    const receive = (url: string) => {
      invitations.current.receive(url);
      openPendingInvitation();
    };
    const subscription = Linking.addEventListener("url", ({ url }) => {
      receivedLiveLink = true;
      receive(url);
    });
    void Linking.getInitialURL().then((url) => {
      if (active && url && !receivedLiveLink) receive(url);
    }).catch(() => undefined);
    return () => { active = false; subscription.remove(); };
  }, [openPendingInvitation]);

  useEffect(() => {
    setSessionExpiredHandler(() => {
      if (navigationRef.isReady()) {
        navigationRef.reset({ index: 0, routes: [{ name: "SignIn" }] });
        Alert.alert("Sesión caducada", "Vuelve a iniciar sesión para continuar.");
      }
    });
    return () => setSessionExpiredHandler(undefined);
  }, []);

  useEffect(() => {
    let active = true;
    restoreSession().then((restored) => {
      if (active) setInitialRoute(restored ? nextAccountScreen() : "SignIn");
    }).catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [attempt]);

  if (!initialRoute) {
    return <Screen><View style={{ flex: 1, gap: 20, justifyContent: "center" }}>
      <Text style={[typography.body, { color: colors.text, textAlign: "center" }]}>{error ? "No se ha podido recuperar la sesión. Comprueba tu conexión." : "Recuperando sesión…"}</Text>
      {showServerWakeupNotice ? <Text accessibilityLiveRegion="polite" style={[typography.body, { color: colors.textMuted, fontSize: 12, textAlign: "center" }]}>{serverWakeupMessage}</Text> : null}
      {error ? <PrimaryButton onPress={() => { setError(false); setAttempt((value) => value + 1); }}>REINTENTAR</PrimaryButton> : null}
    </View></Screen>;
  }
  return (
    <NavigationContainer ref={navigationRef} theme={navigationTheme} onReady={openPendingInvitation} onStateChange={openPendingInvitation}>
      <Stack.Navigator initialRouteName={initialRoute} screenOptions={{ headerShown: false, animation: "fade" }}>
        <Stack.Screen name="Welcome" component={WelcomeScreen} />
        <Stack.Screen name="SignIn" component={SignInScreen} />
        <Stack.Screen name="Schedule" component={ScheduleScreen} />
        <Stack.Screen name="LocationConsent" component={LocationConsentScreen} />
        <Stack.Screen name="AvailabilityConsent" component={AvailabilityConsentScreen} />
        <Stack.Screen name="Profile" component={ProfileScreen} />
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="CreateGameData" component={CreateGameDataScreen} />
        <Stack.Screen name="CreateGameRules" component={CreateGameRulesScreen} />
        <Stack.Screen name="ShareGame" component={ShareGameScreen} />
        <Stack.Screen name="GameInvitation" component={GameInvitationScreen} />
        <Stack.Screen name="GameNorms" component={GameNormsScreen} />
        <Stack.Screen name="WaitingRoom" component={WaitingRoomScreen} />
        <Stack.Screen name="GameStarted" component={GameStartedScreen} />
        <Stack.Screen name="MissionCard" component={MissionCardScreen} />
        <Stack.Screen name="GameCancelled" component={GameCancelledScreen} />
        <Stack.Screen name="ScanInvitation" component={ScanInvitationScreen} />
        <Stack.Screen name="ApiStatus" component={ApiStatusScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
