import { Ionicons } from "@expo/vector-icons";
import { useIsFocused } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

import { ApiError } from "../api/client";
import { getMyGames, startGame } from "../api/game";
import { getPlayers } from "../api/inscription";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { Player } from "../types/inscription";
import type { GameSummary } from "../types/game";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "WaitingRoom">;

const columns = [9, 106, 202];
const rowHeight = 100;
// La sala se recarga al volver a ella y cada pocos segundos, sin tiempo real (referencia-ui, UI25).
const REFRESH_MS = 10_000;
const MIN_PLAYERS = 3;

function memberCountLabel(count: number) {
  return `${count} ${count === 1 ? "inscrito" : "inscritos"}`;
}

function countdownLabel(start: string) {
  const remaining = Math.max(0, new Date(start).getTime() - Date.now());
  const totalSeconds = Math.floor(remaining / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${days} D ${pad(hours)} H ${pad(minutes)} MIN ${pad(seconds)} S`;
}

// Se repinta cada segundo por su cuenta para no recargar la sala entera.
function Countdown({ start }: { start: string }) {
  const [, setTick] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setTick((value) => value + 1), 1000);
    return () => clearInterval(timer);
  }, []);
  return <Text adjustsFontSizeToFit minimumFontScale={0.6} numberOfLines={1} style={[styles.countdownText, styles.countdownCentered]}>{countdownLabel(start)}</Text>;
}

// Tres columnas con un pequeño desfase para que los fantasmas no queden alineados.
function position(index: number) {
  return { left: (columns[index % 3] ?? 9) + (index % 2) * 4, top: Math.floor(index / 3) * rowHeight + (index % 3 === 2 ? 16 : 0) };
}

// Debajo del aviso solo queda Inicio: el botón atrás nunca vuelve a la sala.
function showStarted(navigation: Props["navigation"], game: GameSummary) {
  navigation.reset({ index: 1, routes: [{ name: "Home" }, { name: "GameStarted", params: { game } }] });
}

export function WaitingRoomScreen({ navigation, route }: Props) {
  const { height } = useWindowDimensions();
  const { gameId, game } = route.params;
  const [players, setPlayers] = useState<Player[]>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const isFocused = useIsFocused();
  const [gameState, setGameState] = useState<GameSummary>();
  const [isStarting, setIsStarting] = useState(false);
  const [startError, setStartError] = useState<string>();
  const isMaster = gameState?.soyMaster ?? false;
  const masterPlays = players?.some((player) => player.esMaster) ?? false;
  const canReviewSchedule = !isMaster || masterPlays;
  const canStart = (players?.length ?? 0) >= MIN_PLAYERS;
  const rows = Math.ceil((players?.length ?? 0) / 3);
  const canvasHeight = Math.max(245, Math.min(335, height - 420), rows * rowHeight + 30);

  useEffect(() => {
    if (!isFocused) return undefined;
    const timer = setInterval(() => setAttempt((value) => value + 1), REFRESH_MS);
    return () => clearInterval(timer);
  }, [isFocused]);

  useEffect(() => {
    if (!isFocused) return undefined;
    let active = true;
    void Promise.all([getPlayers(gameId), getMyGames()]).then(([list, games]) => {
      if (!active) return;
      const current = games.find((item) => item.id === gameId);
      if (!current) throw new Error("Partida no disponible");
      setError(undefined);
      setPlayers(list);
      setGameState(current);
      if (current.estado === "EN_JUEGO") showStarted(navigation, current);
      if (current.estado === "CANCELADA") navigation.replace("GameCancelled", { game: current });
    }).catch(() => {
      if (active) setError("No hemos podido cargar los inscritos. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [gameId, attempt, navigation, isFocused]);

  async function start() {
    setStartError(undefined);
    setIsStarting(true);
    try {
      const started = await startGame(gameId);
      showStarted(navigation, started);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.code === "POCOS_JUGADORES") {
        setStartError("Aún no se puede arrancar: hacen falta al menos 3 jugadores inscritos.");
      } else if (requestError instanceof ApiError && requestError.code === "PARTIDA_NO_ABIERTA") {
        setAttempt((value) => value + 1);
      } else {
        setStartError(requestError instanceof ApiError ? requestError.message : "No hemos podido arrancar la partida.");
      }
    } finally {
      setIsStarting(false);
    }
  }

  function goToGames() {
    navigation.reset({
      index: 0,
      routes: [{ name: "Home", params: gameState ? { createdGame: gameState } : undefined }],
    });
  }

  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.content}>
      <View style={styles.header}>
        <Pressable accessibilityLabel="Volver a mis partidas" accessibilityRole="button" onPress={goToGames} style={({ pressed }) => [styles.back, pressed && styles.pressed]}>
          <Ionicons color={colors.text} name="arrow-back" size={22} />
        </Pressable>
        <Text style={styles.title}>SALA DE{"\n"}ESPERA</Text>
        <View style={styles.memberTag}><Text style={styles.memberTagText}>{isMaster ? "Eres el máster" : "Te has unido"}</Text></View>
      </View>
      <View style={styles.countdown}>
        <Countdown start={game.inicioEn} />
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
          <View style={styles.memberCount}><Text style={styles.memberCountText}>{memberCountLabel(players.length)}</Text></View>
        </View>}
      </View>
    </View>
    <View style={styles.footer}>
      {startError ? <Text accessibilityLiveRegion="polite" style={styles.error}>{startError}</Text> : null}
      {canReviewSchedule ? <Pressable accessibilityRole="button" onPress={() => navigation.navigate("Schedule", { gameId })} style={({ pressed }) => [styles.reviewButton, pressed && styles.pressed]}>
        <Text style={styles.reviewText}>REVISAR MI HORARIO</Text>
      </Pressable> : null}
      {isMaster ? <PrimaryButton disabled={isStarting || !canStart} onPress={start}>{isStarting ? "ARRANCANDO…" : "ARRANCAR YA"}</PrimaryButton> : null}
    </View>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 13 },
  header: { alignItems: "center", flexDirection: "row", gap: 10, justifyContent: "space-between" },
  back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 2, height: 42, justifyContent: "center", width: 42 },
  title: { color: colors.text, ...bangers(37), lineHeight: 35 },
  memberTag: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 1, paddingHorizontal: 11, paddingVertical: 6, transform: [{ rotate: "4deg" }] },
  memberTagText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 },
  countdown: { alignItems: "center", backgroundColor: colors.ink, borderRadius: 18, marginTop: 16, paddingHorizontal: 8, paddingVertical: 10 },
  countdownText: { color: colors.lime, ...bangers(35), letterSpacing: 0.6, width: "100%" },
  countdownCaption: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 1 },
  countdownCentered: { textAlign: "center" },
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
  reviewText: { color: colors.text, ...bangers(21), letterSpacing: 0.7 },
  pressed: { opacity: 0.8, transform: [{ scale: 0.98 }] },
  errorBox: { gap: 14, width: "100%" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, textAlign: "center" },
});
