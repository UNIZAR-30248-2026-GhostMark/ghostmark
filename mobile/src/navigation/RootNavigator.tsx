import { DarkTheme, NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";

import { ApiStatusScreen } from "../screens/ApiStatusScreen";
import { HomeScreen } from "../screens/HomeScreen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

const Stack = createNativeStackNavigator<RootStackParamList>();

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
  return (
    <NavigationContainer theme={navigationTheme}>
      <Stack.Navigator screenOptions={{ headerShown: false, animation: "fade" }}>
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="ApiStatus" component={ApiStatusScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
