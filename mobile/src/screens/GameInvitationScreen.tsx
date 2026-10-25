import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getGameByCode } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { PublicGame } from "../types/game";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameInvitation">;
const labels: Record<PublicGame["estado"], string> = {
  EN_INSCRIPCION: "Inscripción abierta", EN_JUEGO: "En juego", CANCELADA: "Cancelada", CERRADA: "Cerrada",
};
const formatDate = (value: string) => new Date(value).toLocaleString("es-ES");
type LoadState = { code: string; attempt: number } & (
  { status: "loading" } | { status: "loaded"; game: PublicGame } | { status: "error"; message: string }
);

export function GameInvitationScreen({ navigation, route }: Props) {
  const { code } = route.params;
  const [state, setState] = useState<LoadState>({ code, attempt: 0, status: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    getGameByCode(code).then((game) => {
      if (active) setState({ code, attempt, status: "loaded", game });
    }).catch((error: unknown) => {
      if (active) setState({ code, attempt, status: "error", message: error instanceof ApiError && error.status === 404
        ? "No existe una partida con este código de invitación."
        : "No hemos podido cargar la partida. Comprueba la conexión e inténtalo de nuevo." });
    });
    return () => { active = false; };
  }, [code, attempt]);

  const loading = state.code !== code || state.attempt !== attempt || state.status === "loading";
  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.heading}>PARTIDA</Text>
        {loading ? <ActivityIndicator accessibilityLabel="Cargando partida" color={colors.lime} size="large" /> : null}
        {!loading && state.status === "loaded" ? <View style={styles.card}>
          <Text style={styles.title}>{state.game.nombre}</Text>
          <Text style={styles.status}>{labels[state.game.estado]}</Text>
          <Text style={styles.detail}>Máster: {state.game.aliasMaster ?? "Cuenta eliminada"}</Text>
          <Text style={styles.detail}>Inicio: {formatDate(state.game.inicioEn)}</Text>
          <Text style={styles.detail}>Fin: {formatDate(state.game.finEn)}</Text>
          <Text style={styles.detail}>Inscritos: {state.game.numInscritos}</Text>
        </View> : null}
        {!loading && state.status === "error" ? <View style={styles.error}>
          <Text accessibilityLiveRegion="polite" style={styles.detail}>{state.message}</Text>
          <PrimaryButton onPress={() => setAttempt((value) => value + 1)}>REINTENTAR</PrimaryButton>
        </View> : null}
      </ScrollView>
      <PrimaryButton onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}>IR A MIS PARTIDAS</PrimaryButton>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, gap: 24, paddingBottom: 24 },
  heading: { color: colors.text, fontFamily: "Bangers", fontSize: 34, paddingRight: 3 },
  card: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 16, borderWidth: 3, gap: 15, padding: 20 },
  title: { color: colors.text, fontFamily: "Bangers", fontSize: 32, paddingRight: 3 },
  status: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 17 },
  detail: { color: colors.text, fontFamily: "ZenMaru", fontSize: 16, lineHeight: 24 },
  error: { gap: 20 },
});
