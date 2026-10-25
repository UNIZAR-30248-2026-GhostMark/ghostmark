import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

import { getCurrentAccount } from "../api/auth";
import { getPlayers } from "../api/inscription";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { Player } from "../types/inscription";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "WaitingRoom">;

const columns = [9, 106, 202];
const rowHeight = 100;

function countdownLabel(start: string) {
  const remaining = Math.max(0, new Date(start).getTime() - Date.now());
  const totalMinutes = Math.floor(remaining / 60_000);
  const days = Math.floor(totalMinutes / (24 * 60));
  const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
  const minutes = totalMinutes % 60;
  return `${days} D ${String(hours).padStart(2, "0")} H ${String(minutes).padStart(2, "0")} MIN`;
}

// Tres columnas con un pequeño desfase para que los fantasmas no queden alineados.
function position(index: number) {
  return { left: (columns[index % 3] ?? 9) + (index % 2) * 4, top: Math.floor(index / 3) * rowHeight + (index % 3 === 2 ? 16 : 0) };
}

export function WaitingRoomScreen({ navigation, route }: Props) {
  const { height } = useWindowDimensions();
  const { gameId, game } = route.params;
  const [players, setPlayers] = useState<Player[]>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const alias = getCurrentAccount()?.alias;
  const isMaster = players?.some((player) => player.esMaster && player.alias === alias) ?? false;
  const rows = Math.ceil((players?.length ?? 0) / 3);
  const canvasHeight = Math.max(245, Math.min(335, height - 420), rows * rowHeight + 30);

  useEffect(() => {
    let active = true;
    getPlayers(gameId).then((list) => {
      if (active) setPlayers(list);
    }).catch(() => {
      if (active) setError("No hemos podido cargar los inscritos. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [gameId, attempt]);

  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.content}>
      <View style={styles.header}>
        <Text style={styles.title}>SALA DE{"\n"}ESPERA</Text>
        <View style={styles.memberTag}><Text style={styles.memberTagText}>{isMaster ? "Eres el máster" : "Te has unido"}</Text></View>
      </View>
      <View style={styles.countdown}>
        <Text adjustsFontSizeToFit minimumFontScale={0.72} numberOfLines={1} style={styles.countdownText}>{countdownLabel(game.inicioEn)}</Text>
        <Text style={styles.countdownCaption}>para empezar</Text>
      </View>
      <View style={styles.playerArea}>
        {error ? <View style={styles.errorBox}>
          <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text>
          <PrimaryButton onPress={() => { setError(undefined); setAttempt((value) => value + 1); }}>REINTENTAR</PrimaryButton>
        </View> : !players ? <ActivityIndicator accessibilityLabel="Cargando inscritos" color={colors.lime} size="large" /> : <View style={[styles.playerCanvas, { height: canvasHeight }]}>
          {players.map((player, index) => <View key={player.id} style={[styles.player, position(index)]}>
            <Image accessibilityLabel={player.alias ?? "Cuenta eliminada"} source={require("../../assets/ghosts/fantasma-saluda.png")} style={[styles.ghost, index % 2 === 1 && styles.ghostFlip]} />
            <Text style={styles.playerName}>{player.alias ?? "Cuenta eliminada"}</Text>
          </View>)}
          <View style={styles.memberCount}><Text style={styles.memberCountText}>{`${players.length} inscritos`}</Text></View>
        </View>}
      </View>
    </View>
    <View style={styles.footer}>
      <Pressable accessibilityRole="button" onPress={() => navigation.navigate("Schedule", { gameId })} style={({ pressed }) => [styles.reviewButton, pressed && styles.pressed]}>
        <Text style={styles.reviewText}>REVISAR MI HORARIO</Text>
      </Pressable>
      {isMaster ? <PrimaryButton disabled onPress={() => undefined}>ARRANCAR YA</PrimaryButton> : null}
    </View>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 13 },
  header: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" },
  title: { color: colors.text, fontFamily: "Bangers", fontSize: 37, lineHeight: 35 },
  memberTag: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 1, paddingHorizontal: 11, paddingVertical: 6, transform: [{ rotate: "4deg" }] },
  memberTagText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 },
  countdown: { alignItems: "center", backgroundColor: colors.ink, borderRadius: 18, marginTop: 16, paddingHorizontal: 8, paddingVertical: 10 },
  countdownText: { color: colors.lime, fontFamily: "Bangers", fontSize: 35, letterSpacing: 0.6, width: "100%" },
  countdownCaption: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 1 },
  playerArea: { alignItems: "center", flex: 1, justifyContent: "center", marginTop: 8 },
  playerCanvas: { position: "relative", width: 300 },
  player: { alignItems: "center", position: "absolute", width: 88 },
  ghost: { height: 70, resizeMode: "contain", width: 82 },
  ghostFlip: { transform: [{ scaleX: -1 }] },
  playerName: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 9, borderWidth: 1, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: -6, paddingHorizontal: 8, paddingVertical: 2 },
  memberCount: { alignSelf: "center", backgroundColor: colors.textMuted, borderColor: colors.ink, borderRadius: 9, borderWidth: 1, bottom: 4, left: 108, paddingHorizontal: 12, paddingVertical: 4, position: "absolute" },
  memberCountText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 12 },
  footer: { gap: 13, paddingBottom: 10, paddingHorizontal: 24, paddingTop: 8 },
  reviewButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 15, borderWidth: 3, paddingVertical: 12, shadowColor: colors.ink, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 0.7, shadowRadius: 0 },
  reviewText: { color: colors.text, fontFamily: "Bangers", fontSize: 21, letterSpacing: 0.7 },
  pressed: { opacity: 0.8, transform: [{ scale: 0.98 }] },
  errorBox: { gap: 14, width: "100%" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, textAlign: "center" },
});
