import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Alert, Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getCurrentAccount } from "../api/auth";
import { closeGame, getMyGames } from "../api/game";
import { getPlayers } from "../api/inscription";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { GameSummary } from "../types/game";
import type { Player } from "../types/inscription";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameTab">;

export function GameTabScreen({ navigation, route }: Props) {
  const [game, setGame] = useState<GameSummary>();
  const [players, setPlayers] = useState<Player[]>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    let active = true;
    void Promise.all([getMyGames(), getPlayers(route.params.gameId)]).then(([games, members]) => {
      const current = games.find((item) => item.id === route.params.gameId);
      if (!current) throw new Error("Partida no disponible");
      if (!active) return;
      if (current.estado === "CERRADA") {
        navigation.replace("GameEnded", { game: current });
        return;
      }
      setGame(current);
      setPlayers(members);
      setError(undefined);
    }).catch((requestError: unknown) => {
      if (active) setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar la partida. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [attempt, navigation, route.params.gameId]);

  function confirmFinish() {
    // Cerrar no se puede deshacer y termina la partida para todos (R11-24).
    Alert.alert("¿Cerrar la partida?", "Termina para todos y no se puede deshacer.", [
      { style: "cancel", text: "Cancelar" },
      { style: "destructive", text: "Cerrar", onPress: () => void finishGame() },
    ]);
  }

  async function finishGame() {
    setError(undefined);
    setIsClosing(true);
    try {
      const closed = await closeGame(route.params.gameId);
      navigation.replace("GameEnded", { game: closed });
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cerrar la partida. Inténtalo de nuevo.");
    } finally {
      setIsClosing(false);
    }
  }

  return <Screen padded={false}><View style={styles.screen}>
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <Tabs game={game} navigation={navigation} />
      {error ? <View style={styles.errorState}><Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text><PrimaryButton onPress={() => setAttempt((value) => value + 1)}>REINTENTAR</PrimaryButton></View>
        : !game || !players ? <View style={styles.loading}><ActivityIndicator color={colors.lime} size="large" /><Text style={styles.loadingText}>CARGANDO PARTIDA…</Text></View>
          : <>{game.soyMaster ? <MasterTasks /> : null}<View style={styles.playersHeading}><Text style={styles.sectionTitle}>JUGADORES</Text>{players.every((player) => player.estado) ? <Text style={styles.count}>{players.filter((player) => player.estado === "VIVO").length} vivos de {players.length}</Text> : null}</View><View style={styles.players}>{players.map((player) => <PlayerRow currentAlias={getCurrentAccount()?.alias} key={player.id} player={player} />)}</View></>}
    </ScrollView>
    {game ? <Actions isClosing={isClosing} isMaster={game?.soyMaster ?? false} onClose={confirmFinish} /> : null}
  </View></Screen>;
}

function Tabs({ game, navigation }: { game?: GameSummary; navigation: Props["navigation"] }) {
  return <View style={styles.tabs}>
    {game?.soyJugador ? <Pressable accessibilityLabel="Ir a mi misión" accessibilityRole="button" onPress={() => navigation.navigate("MissionCard", { gameId: game.id })} style={styles.tab}><Text style={styles.tabText}>Misión</Text></Pressable> : <View style={styles.tab}><Text style={styles.tabText}>Misión</Text></View>}
    {game ? <Pressable accessibilityLabel="Ir al feed" accessibilityRole="button" onPress={() => navigation.navigate("Feed", { gameId: game.id, isPlayer: game.soyJugador })} style={styles.tab}><Text style={styles.tabText}>Feed</Text></Pressable> : <View style={styles.tab}><Text style={styles.tabText}>Feed</Text></View>}
    <View style={styles.tabOn}><Text style={styles.tabOnText}>PARTIDA</Text></View>
  </View>;
}

function MasterTasks() {
  return <View><Text style={styles.sectionTitle}>TAREAS DE MÁSTER</Text><View style={styles.taskRow}><TaskCard color="white" label="Disputas" /><TaskCard color="lavender" label="Misiones IA" /><TaskCard color="purple" label="Reportes" /></View></View>;
}

function TaskCard({ color, label }: { color: "white" | "lavender" | "purple"; label: string }) {
  const colorStyle = color === "white" ? styles.taskWhite : color === "lavender" ? styles.taskLavender : styles.taskPurple;
  const textStyle = color === "purple" ? styles.taskTextLight : undefined;
  return <View style={[styles.taskCard, colorStyle]}><Text style={[styles.taskNumber, textStyle]}>0</Text><Text style={[styles.taskLabel, textStyle]}>{label}</Text></View>;
}

function PlayerRow({ currentAlias, player }: { currentAlias?: string; player: Player }) {
  // Sin estado (el servidor aún no lo manda) no se pinta: no se sabe si está vivo.
  const state = player.estado;
  const inactive = state === "ELIMINADO";
  const isCurrentPlayer = player.alias !== null && player.alias === currentAlias;
  return <View style={[styles.playerRow, inactive && styles.playerInactive]}><Image source={require("../../assets/ghosts/fantasma-saluda.png")} style={[styles.playerGhost, inactive && styles.playerGhostInactive]} /><Text numberOfLines={1} style={styles.playerName}>{player.alias ?? "Cuenta eliminada"}{isCurrentPlayer ? " (tú)" : ""}{player.esMaster ? " (máster)" : ""}</Text>{state ? <Text style={[styles.playerState, state === "VIVO" ? styles.playerAlive : styles.playerEliminated]}>{state}</Text> : null}</View>;
}

function Actions({ isClosing, isMaster, onClose }: { isClosing: boolean; isMaster: boolean; onClose: () => void }) {
  return <View style={styles.footer}><View style={styles.actionRow}><Pressable accessibilityLabel="Reportar" accessibilityRole="button" disabled style={styles.reportButton}><Text style={styles.reportText}>REPORTAR</Text></Pressable><Pressable accessibilityLabel="Abandonar" accessibilityRole="button" disabled style={styles.abandonButton}><Text style={styles.abandonText}>ABANDONAR</Text></Pressable>{isMaster ? <Pressable accessibilityLabel="Cerrar partida" accessibilityRole="button" disabled={isClosing} onPress={onClose} style={({ pressed }) => [styles.closeButton, (pressed || isClosing) && styles.pressed]}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.closeText}>{isClosing ? "CERRANDO…" : "CERRAR PARTIDA"}</Text></Pressable> : null}</View></View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, content: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 18 },
  tabs: { backgroundColor: colors.ink, borderRadius: 15, flexDirection: "row", height: 49, padding: 4 }, tab: { alignItems: "center", flex: 1, justifyContent: "center" }, tabOn: { alignItems: "center", backgroundColor: colors.white, borderColor: "#CFC2DD", borderRadius: 11, borderWidth: 2, flex: 1, justifyContent: "center" }, tabText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 }, tabOnText: { color: colors.ink, ...bangers(18) },
  sectionTitle: { color: colors.text, ...bangers(28), marginTop: 22 }, taskRow: { flexDirection: "row", gap: 9, marginTop: 9 }, taskCard: { borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1, minHeight: 70, paddingHorizontal: 10, paddingVertical: 7 }, taskWhite: { backgroundColor: colors.white }, taskLavender: { backgroundColor: "#D7C6FF" }, taskPurple: { backgroundColor: colors.surfaceRaised, borderColor: colors.border }, taskNumber: { color: colors.ink, ...bangers(29), lineHeight: 30 }, taskLabel: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: 1 }, taskTextLight: { color: colors.white },
  playersHeading: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" }, count: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: 22, paddingHorizontal: 9, paddingVertical: 4 }, players: { gap: 7, marginTop: 10, paddingBottom: 22 }, playerRow: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", minHeight: 54, paddingHorizontal: 8 }, playerInactive: { backgroundColor: "#38284B", borderColor: "#635173" }, playerGhost: { height: 39, resizeMode: "contain", width: 42 }, playerGhostInactive: { opacity: 0.38, tintColor: "#D4CBDC" }, playerName: { color: colors.text, flex: 1, fontFamily: "ZenMaruBold", fontSize: 14, marginLeft: 5 }, playerState: { borderColor: colors.ink, borderRadius: 7, borderWidth: 1, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 9, paddingHorizontal: 7, paddingVertical: 4 }, playerAlive: { backgroundColor: colors.lime }, playerEliminated: { backgroundColor: colors.danger },
  footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 }, actionRow: { flexDirection: "row", gap: 7 }, reportButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, flex: 1, justifyContent: "center", minHeight: 50, paddingHorizontal: 5 }, abandonButton: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1.1, justifyContent: "center", minHeight: 50, paddingHorizontal: 5 }, closeButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1.45, justifyContent: "center", minHeight: 50, paddingHorizontal: 6 }, reportText: { color: colors.white, ...bangers(15), textAlign: "center" }, abandonText: { color: colors.ink, ...bangers(15), textAlign: "center" }, closeText: { alignSelf: "stretch", color: colors.ink, ...bangers(15), textAlign: "center" }, loading: { alignItems: "center", gap: 13, marginTop: 80 }, loadingText: { color: colors.text, ...bangers(24) }, errorState: { alignItems: "center", gap: 16, marginTop: 70 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 20, textAlign: "center" }, pressed: { opacity: 0.78, transform: [{ scale: 0.97 }] },
});
