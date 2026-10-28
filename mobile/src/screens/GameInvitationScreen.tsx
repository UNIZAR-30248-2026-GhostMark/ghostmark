import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Image, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getGameByCode } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { PublicGame } from "../types/game";
import type { RootStackParamList } from "../types/navigation";
import { formatGameDate } from "../utils/gameDate";

type Props = NativeStackScreenProps<RootStackParamList, "GameInvitation">;
const labels: Record<PublicGame["estado"], string> = {
  EN_INSCRIPCION: "Inscripción abierta", EN_JUEGO: "En juego", CANCELADA: "Cancelada", CERRADA: "Cerrada",
};
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
  const lateGame = !loading && state.status === "loaded" && state.game.estado === "EN_JUEGO";
  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        {!lateGame ? <Text style={styles.heading}>PARTIDA</Text> : null}
        {loading ? <ActivityIndicator accessibilityLabel="Cargando partida" color={colors.lime} size="large" /> : null}
        {!loading && state.status === "loaded" && state.game.estado === "EN_JUEGO" ? <LateInvitation game={state.game} /> : null}
        {!loading && state.status === "loaded" && state.game.estado !== "EN_JUEGO" ? <View style={styles.card}>
          <Text style={styles.title}>{state.game.nombre}</Text>
          <Text style={styles.status}>{labels[state.game.estado]}</Text>
          <Text style={styles.detail}>Máster: {state.game.aliasMaster ?? "Cuenta eliminada"}</Text>
          <Text style={styles.detail}>Inicio: {formatGameDate(state.game.inicioEn)}</Text>
          <Text style={styles.detail}>Fin: {formatGameDate(state.game.finEn)}</Text>
          <Text style={styles.detail}>Inscritos: {state.game.numInscritos}</Text>
        </View> : null}
        {!loading && state.status === "error" ? <View style={styles.error}>
          <Text accessibilityLiveRegion="polite" style={styles.detail}>{state.message}</Text>
          <PrimaryButton onPress={() => setAttempt((value) => value + 1)}>REINTENTAR</PrimaryButton>
        </View> : null}
      </ScrollView>
      {!loading && state.status === "loaded" && state.game.estado === "EN_INSCRIPCION"
        ? <PrimaryButton onPress={() => navigation.replace("GameNorms", { code, game: state.game })}>VER NORMAS</PrimaryButton>
        : !loading && state.status === "loaded" && state.game.estado === "CANCELADA"
            ? <PrimaryButton onPress={() => navigation.replace("GameCancelled", { game: state.game })}>VER PARTIDA</PrimaryButton>
            : <PrimaryButton onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}>{!loading && state.status === "loaded" && state.game.estado === "EN_JUEGO" ? "VOLVER A MIS PARTIDAS" : "IR A MIS PARTIDAS"}</PrimaryButton>}
    </Screen>
  );
}

function LateInvitation({ game }: { game: PublicGame }) {
  return <View style={styles.lateInvitation}>
    <Image accessibilityLabel="Fantasma despidiéndose" source={require("../../assets/ghosts/fantasma-chao.png")} style={styles.lateGhost} />
    <View style={styles.lateTag}><Text style={styles.lateTagText}>En juego</Text></View>
    <Text style={styles.lateTitle}>¡LLEGAS TARDE!{"\n"}ESTA PARTIDA YA{"\n"}HA EMPEZADO</Text>
    <Text numberOfLines={2} style={styles.lateGameName}>{game.nombre}</Text>
    <Text style={styles.lateDetail}>No admite jugadores nuevos. Pídele al máster que te avise de la próxima.</Text>
  </View>;
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, gap: 24, paddingBottom: 24 },
  heading: { color: colors.text, ...bangers(34) },
  card: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 16, borderWidth: 3, gap: 15, padding: 20 },
  title: { color: colors.text, ...bangers(32) },
  status: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 17 },
  detail: { color: colors.text, fontFamily: "ZenMaru", fontSize: 16, lineHeight: 24 },
  error: { gap: 20 },
  lateInvitation: { alignItems: "center", flex: 1, justifyContent: "center", paddingHorizontal: 10 },
  lateGhost: { height: 184, marginBottom: 6, resizeMode: "contain", width: 190 },
  lateTag: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 8, borderWidth: 1, paddingHorizontal: 12, paddingVertical: 5 },
  lateTagText: { color: colors.ink, fontFamily: "ZenMaruBlack", fontSize: 11 },
  lateTitle: { alignSelf: "stretch", color: colors.text, ...bangers(34), lineHeight: 36, marginTop: 18, textAlign: "center" },
  lateGameName: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 16, marginTop: 14, textAlign: "center" },
  lateDetail: { color: colors.text, fontFamily: "ZenMaru", fontSize: 14, lineHeight: 21, marginTop: 18, textAlign: "center" },
});
