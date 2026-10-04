import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";

import { getConfiguredApiUrl } from "../api/client";
import { getHealth } from "../api/health";
import { GhostButton } from "../components/GhostButton";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "ApiStatus">;
type CheckState = "idle" | "loading" | "success" | "error";

export function ApiStatusScreen({ navigation }: Props) {
  const [state, setState] = useState<CheckState>("idle");
  const [message, setMessage] = useState("Todavía no se ha realizado ninguna petición.");

  async function checkServer() {
    setState("loading");
    setMessage("Consultando el servidor local…");

    try {
      const health = await getHealth();
      setState("success");
      setMessage(`Servidor disponible: ${health.status}`);
    } catch (error) {
      setState("error");
      setMessage(error instanceof Error ? error.message : "No se pudo contactar con el servidor.");
    }
  }

  return (
    <View style={styles.container}>
      <Text style={typography.title}>CONEXIÓN API</Text>
      <Text style={styles.description}>Endpoint de comprobación: /api/v1/health</Text>
      <Text selectable style={styles.url}>{getConfiguredApiUrl()}</Text>

      <View style={[styles.result, state === "success" && styles.success, state === "error" && styles.error]}>
        {state === "loading" ? <ActivityIndicator color={colors.lime} /> : null}
        <Text style={styles.resultText}>{message}</Text>
      </View>

      <GhostButton disabled={state === "loading"} onPress={checkServer}>PROBAR CONEXIÓN</GhostButton>
      <GhostButton onPress={() => navigation.goBack()} variant="secondary">VOLVER</GhostButton>

      <Text style={styles.help}>
        En un móvil físico configura EXPO_PUBLIC_API_URL con la IP LAN del ordenador y asegúrate de que ambos estén en la misma red.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { backgroundColor: colors.background, flex: 1, gap: 18, justifyContent: "center", padding: 28 },
  description: { ...typography.body, color: colors.textMuted },
  url: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 14 },
  result: {
    alignItems: "center",
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 2,
    gap: 10,
    minHeight: 116,
    justifyContent: "center",
    padding: 18,
  },
  success: { borderColor: colors.lime },
  error: { borderColor: colors.danger },
  resultText: { ...typography.body, textAlign: "center" },
  help: { ...typography.body, color: colors.textMuted, fontSize: 13, lineHeight: 20, marginTop: 4 },
});
