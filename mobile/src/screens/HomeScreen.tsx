import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { StyleSheet, Text, View } from "react-native";

import { GhostButton } from "../components/GhostButton";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

export function HomeScreen({ navigation }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.mark} />
      <Text style={typography.title}>GHOSTMARK</Text>
      <Text style={styles.subtitle}>El juego de eliminaciones seguro para tu campus.</Text>

      <View style={styles.card}>
        <Text style={typography.heading}>Esqueleto de la app</Text>
        <Text style={typography.body}>
          Expo, TypeScript, navegación y tema visual están preparados para las siguientes tareas.
        </Text>
      </View>

      <GhostButton onPress={() => navigation.navigate("Schedule")}>MI HORARIO</GhostButton>
      <GhostButton onPress={() => navigation.navigate("ApiStatus")}>COMPROBAR SERVIDOR</GhostButton>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    flex: 1,
    gap: 22,
    justifyContent: "center",
    padding: 28,
  },
  mark: {
    alignSelf: "center",
    backgroundColor: colors.lime,
    borderRadius: 999,
    height: 12,
    width: 88,
  },
  subtitle: { ...typography.body, color: colors.textMuted, lineHeight: 24 },
  card: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderRadius: 18,
    borderWidth: 2,
    gap: 12,
    padding: 20,
  },
});
