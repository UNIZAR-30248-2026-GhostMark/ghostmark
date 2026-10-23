import { createNavigationContainerRef, DarkTheme, NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { Alert, Button, Text, View } from "react-native";

import { restoreSession, setSessionExpiredHandler } from "../api/auth";
import { ApiStatusScreen } from "../screens/ApiStatusScreen";
import { HomeScreen } from "../screens/HomeScreen";
import { SignInScreen } from "../screens/SignInScreen";
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
  const [initialRoute, setInitialRoute] = useState<"Welcome" | "Home">();
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
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
      if (active) setInitialRoute(restored ? "Home" : "Welcome");
    }).catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [attempt]);
  if (!initialRoute) return <View style={{ flex: 1, justifyContent: "center", backgroundColor: colors.background, padding: 24 }}>
    <Text style={{ color: colors.text, textAlign: "center" }}>{error ? "No se ha podido recuperar la sesión. Comprueba tu conexión." : "Recuperando sesión…"}</Text>
    {error ? <Button title="Reintentar" onPress={() => { setError(false); setAttempt((value) => value + 1); }} /> : null}
  </View>;
  return (
    <NavigationContainer ref={navigationRef} theme={navigationTheme}>
      <Stack.Navigator initialRouteName={initialRoute} screenOptions={{ headerShown: false, animation: "fade" }}>
        <Stack.Screen name="Welcome" component={WelcomeScreen} />
        <Stack.Screen name="SignIn" component={SignInScreen} />
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="ApiStatus" component={ApiStatusScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
