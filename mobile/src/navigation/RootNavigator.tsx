import { createNavigationContainerRef, DarkTheme, NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Button, Linking, Text, View } from "react-native";

import { getCurrentAccount, nextAccountScreen, restoreSession, setSessionExpiredHandler } from "../api/auth";
import { ApiStatusScreen } from "../screens/ApiStatusScreen";
import { AvailabilityConsentScreen } from "../screens/AvailabilityConsentScreen";
import { CreateGameDataScreen } from "../screens/CreateGameDataScreen";
import { CreateGameRulesScreen } from "../screens/CreateGameRulesScreen";
import { GameInvitationScreen } from "../screens/GameInvitationScreen";
import { ShareGameScreen } from "../screens/ShareGameScreen";
import { createInvitationInbox } from "../utils/invitation";
import { HomeScreen } from "../screens/HomeScreen";
import { LocationConsentScreen } from "../screens/LocationConsentScreen";
import { ProfileScreen } from "../screens/ProfileScreen";
import { SignInScreen } from "../screens/SignInScreen";
import { ScheduleScreen } from "../screens/ScheduleScreen";
import { WelcomeScreen } from "../screens/WelcomeScreen";
import { colors } from "../theme/colors";
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
  const [initialRoute, setInitialRoute] = useState<"Welcome" | "Home" | "LocationConsent" | "AvailabilityConsent">();
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const invitations = useRef(createInvitationInbox());

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
        navigationRef.reset({ index: 0, routes: [{ name: "Welcome" }] });
        Alert.alert("Sesión caducada", "Vuelve a iniciar sesión para continuar.");
      }
    });
    return () => setSessionExpiredHandler(undefined);
  }, []);

  useEffect(() => {
    let active = true;
    restoreSession().then((restored) => {
      if (active) setInitialRoute(restored ? nextAccountScreen() : "Welcome");
    }).catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [attempt]);

  if (!initialRoute) {
    return <View style={{ flex: 1, justifyContent: "center", backgroundColor: colors.background, padding: 24 }}>
      <Text style={{ color: colors.text, textAlign: "center" }}>{error ? "No se ha podido recuperar la sesión. Comprueba tu conexión." : "Recuperando sesión…"}</Text>
      {error ? <Button title="Reintentar" onPress={() => { setError(false); setAttempt((value) => value + 1); }} /> : null}
    </View>;
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
        <Stack.Screen name="ApiStatus" component={ApiStatusScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
