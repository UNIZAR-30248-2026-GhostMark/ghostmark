import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getCurrentAccount } from "../api/auth";
import { closeGame, getDisputes, getMyGames, getSafeZones } from "../api/game";
import { MapZones } from "../components/MapZones";
import { getPlayers } from "../api/inscription";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { GameSummary, SafeZone } from "../types/game";
import type { Player } from "../types/inscription";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameTab">;

export function GameTabScreen({ navigation, route }: Props) {
  const [game, setGame] = useState<GameSummary>();
  const [players, setPlayers] = useState<Player[]>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const [isClosing, setIsClosing] = useState(false);
  const [hasDisputes, setHasDisputes] = useState(false);

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
      void getDisputes(current.id).then((disputes) => { if (active) setHasDisputes(disputes.length > 0); }).catch(() => { if (active) setHasDisputes(false); });
    }).catch((requestError: unknown) => {
      if (active) setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar la partida. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [attempt, navigation, route.params.gameId]);

  const [confirmingClose, setConfirmingClose] = useState(false);

  function confirmFinish() {
    // Cerrar no se puede deshacer y termina la partida para todos (R11-24).
    setConfirmingClose(true);
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
          : <>{game.soyMaster ? <MasterTasks disputes={game.disputasPorArbitrar ?? 0} /> : null}<ZonesPanel gameId={game.id} navigation={navigation} /><View style={styles.playersHeading}><Text style={styles.sectionTitle}>JUGADORES</Text>{players.every((player) => player.estado) ? <Text style={styles.count}>{players.filter((player) => player.estado === "VIVO").length} vivos de {players.length}</Text> : null}</View><View style={styles.players}>{players.map((player) => <PlayerRow currentAlias={getCurrentAccount()?.alias} key={player.id} player={player} />)}</View></>}
    </ScrollView>
    {game ? <Actions gameId={game.id} hasDisputes={hasDisputes} isClosing={isClosing} isMaster={game.soyMaster} navigation={navigation} onClose={confirmFinish} /> : null}
    <ConfirmDialog confirmLabel="Cerrar" destructive message="Termina para todos y no se puede deshacer." onCancel={() => setConfirmingClose(false)} onConfirm={() => { setConfirmingClose(false); void finishGame(); }} title="¿Cerrar la partida?" visible={confirmingClose} />
  </View></Screen>;
}

function Tabs({ game, navigation }: { game?: GameSummary; navigation: Props["navigation"] }) {
  return <View style={styles.topNavigation}>
    <Pressable accessibilityLabel="Volver a mis partidas" accessibilityRole="button" onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })} style={styles.homeButton}><Text style={styles.homeButtonText}>←</Text></Pressable>
    <View style={styles.tabs}>
      {/* El máster que no juega no tiene misión: su barra es Feed y Partida (R11-36). */}
      {game?.soyJugador ? <Pressable accessibilityLabel="Ir a mi misión" accessibilityRole="button" onPress={() => navigation.navigate("MissionCard", { gameId: game.id })} style={styles.tab}><Text style={styles.tabText}>Misión</Text></Pressable> : !game ? <View style={styles.tab}><Text style={styles.tabText}>Misión</Text></View> : null}
      {game ? <Pressable accessibilityLabel="Ir al feed" accessibilityRole="button" onPress={() => navigation.navigate("Feed", { gameId: game.id, isPlayer: game.soyJugador })} style={styles.tab}><Text style={styles.tabText}>Feed</Text></Pressable> : <View style={styles.tab}><Text style={styles.tabText}>Feed</Text></View>}
      <View style={styles.tabOn}><Text style={styles.tabOnText}>PARTIDA</Text></View>
    </View>
  </View>;
}

function MasterTasks({ disputes }: { disputes: number }) {
  // Son contadores informativos: la lista se abre siempre con el botón grande del pie.
  return <View><Text style={styles.sectionTitle}>TAREAS DE MÁSTER</Text><View style={styles.taskRow}><TaskCard color="white" label="Disputas" number={disputes} /><TaskCard color="lavender" label="Misiones IA" /><TaskCard color="purple" label="Reportes" /></View></View>;
}

function TaskCard({ color, label, number = 0 }: { color: "white" | "lavender" | "purple"; label: string; number?: number }) {
  const colorStyle = color === "white" ? styles.taskWhite : color === "lavender" ? styles.taskLavender : styles.taskPurple;
  const textStyle = color === "purple" ? styles.taskTextLight : undefined;
  return <View style={[styles.taskCard, colorStyle]}><Text style={[styles.taskNumber, textStyle]}>{number}</Text><Text style={[styles.taskLabel, textStyle]}>{label}</Text></View>;
}

function ZonesPanel({ gameId, navigation }: { gameId: string; navigation: Props["navigation"] }) {
  const [zones, setZones] = useState<SafeZone[]>();
  useEffect(() => {
    let active = true;
    getSafeZones(gameId).then((result) => { if (active) setZones(result.zonas); }).catch(() => { if (active) setZones([]); });
    return () => { active = false; };
  }, [gameId]);
  if (!zones?.length) return null;
  return <View><View style={styles.zonesHeading}><Text style={styles.sectionTitle}>ZONAS SEGURAS</Text><Text style={styles.zoneCount}>{zones.length} {zones.length === 1 ? "zona" : "zonas"}</Text></View><View style={styles.mapPreview}><MapZones height={194} interactive={false} zones={zones} /><Pressable accessibilityHint="Abre el mapa a pantalla completa" accessibilityLabel="Ver mapa de zonas seguras" accessibilityRole="button" onPress={() => navigation.navigate("SafeZonesMap", { zones })} style={styles.mapTapTarget}><View pointerEvents="none" style={styles.mapPreviewHint}><Text style={styles.mapPreviewText}>VER MAPA</Text></View></Pressable></View><View style={styles.zoneNote}><Text style={styles.zoneShield}>⌾</Text><Text style={styles.zoneNoteText}>No se elimina dentro de una zona. Nadie ve dónde estás.</Text></View></View>;
}

function PlayerRow({ currentAlias, player }: { currentAlias?: string; player: Player }) {
  // Sin estado (el servidor aún no lo manda) no se pinta: no se sabe si está vivo.
  const state = player.estado;
  const inactive = state === "ELIMINADO";
  const isCurrentPlayer = player.alias !== null && player.alias === currentAlias;
  return <View style={[styles.playerRow, inactive && styles.playerInactive]}><Image source={require("../../assets/ghosts/fantasma-saluda.png")} style={[styles.playerGhost, inactive && styles.playerGhostInactive]} /><Text numberOfLines={1} style={styles.playerName}>{player.alias ?? "Cuenta eliminada"}{isCurrentPlayer ? " (tú)" : ""}{player.esMaster ? " (máster)" : ""}</Text>{state ? <Text style={[styles.playerState, state === "VIVO" ? styles.playerAlive : styles.playerEliminated]}>{state}</Text> : null}</View>;
}

function Actions({ gameId, hasDisputes, isClosing, isMaster, navigation, onClose }: { gameId: string; hasDisputes: boolean; isClosing: boolean; isMaster: boolean; navigation: Props["navigation"]; onClose: () => void }) {
  return <View style={styles.footer}>{(isMaster || hasDisputes) ? <Pressable accessibilityLabel="Ver disputas" accessibilityRole="button" onPress={() => navigation.navigate("DisputeList", { gameId })} style={({ pressed }) => [styles.disputesButton, pressed && styles.pressed]}><Text style={styles.disputesText}>VER DISPUTAS</Text></Pressable> : null}<View style={styles.actionRow}><Pressable accessibilityLabel="Reportar" accessibilityRole="button" disabled style={styles.reportButton}><Text style={styles.reportText}>REPORTAR</Text></Pressable><Pressable accessibilityLabel="Abandonar" accessibilityRole="button" disabled style={styles.abandonButton}><Text style={styles.abandonText}>ABANDONAR</Text></Pressable>{isMaster ? <Pressable accessibilityLabel="Cerrar partida" accessibilityRole="button" disabled={isClosing} onPress={onClose} style={({ pressed }) => [styles.closeButton, (pressed || isClosing) && styles.pressed]}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.closeText}>{isClosing ? "CERRANDO…" : "CERRAR PARTIDA"}</Text></Pressable> : null}</View></View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, content: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 18 },
  topNavigation: { alignItems: "center", flexDirection: "row", gap: 8 }, homeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 2, height: 45, justifyContent: "center", width: 45 }, homeButtonText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 27, lineHeight: 32, marginTop: -2 }, tabs: { backgroundColor: colors.ink, borderRadius: 15, flex: 1, flexDirection: "row", height: 49, padding: 4 }, tab: { alignItems: "center", flex: 1, justifyContent: "center" }, tabOn: { alignItems: "center", backgroundColor: colors.white, borderColor: "#CFC2DD", borderRadius: 11, borderWidth: 2, flex: 1, justifyContent: "center" }, tabText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 }, tabOnText: { color: colors.ink, ...bangers(18) },
  sectionTitle: { color: colors.text, ...bangers(28), marginTop: 22 }, taskRow: { flexDirection: "row", gap: 9, marginTop: 9 }, taskCard: { borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1, minHeight: 70, paddingHorizontal: 10, paddingVertical: 7 }, taskWhite: { backgroundColor: colors.white }, taskLavender: { backgroundColor: "#D7C6FF" }, taskPurple: { backgroundColor: colors.surfaceRaised, borderColor: colors.border }, taskNumber: { color: colors.ink, ...bangers(29), lineHeight: 37 }, taskLabel: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: 1 }, taskTextLight: { color: colors.white },
  playersHeading: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" }, count: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: 22, paddingHorizontal: 9, paddingVertical: 4 }, players: { gap: 7, marginTop: 10, paddingBottom: 22 }, playerRow: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", minHeight: 54, paddingHorizontal: 8 }, playerInactive: { backgroundColor: "#38284B", borderColor: "#635173" }, playerGhost: { height: 39, resizeMode: "contain", width: 42 }, playerGhostInactive: { opacity: 0.38, tintColor: "#D4CBDC" }, playerName: { color: colors.text, flex: 1, fontFamily: "ZenMaruBold", fontSize: 14, marginLeft: 5 }, playerState: { borderColor: colors.ink, borderRadius: 7, borderWidth: 1, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 9, paddingHorizontal: 7, paddingVertical: 4 }, playerAlive: { backgroundColor: colors.lime }, playerEliminated: { backgroundColor: colors.danger },
  zonesHeading: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" }, zoneCount: { backgroundColor: "#D7C6FF", borderColor: colors.ink, borderRadius: 8, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 10, marginTop: 22, paddingHorizontal: 9, paddingVertical: 4 }, mapPreview: { borderRadius: 19, marginTop: 7, overflow: "hidden", position: "relative" }, mapTapTarget: { bottom: 0, left: 0, position: "absolute", right: 0, top: 0, zIndex: 3 }, mapPreviewHint: { alignItems: "center", backgroundColor: "rgba(18,6,31,.77)", borderColor: colors.lime, borderRadius: 9, borderWidth: 2, bottom: 11, left: 11, paddingHorizontal: 11, paddingVertical: 5, position: "absolute", right: 11 }, mapPreviewText: { color: colors.lime, ...bangers(16), lineHeight: 19, textAlign: "center" }, zoneNote: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", gap: 8, marginTop: 13, minHeight: 58, paddingHorizontal: 12 }, zoneShield: { color: colors.lime, fontSize: 23 }, zoneNoteText: { color: colors.text, flex: 1, fontFamily: "ZenMaruBold", fontSize: 11, lineHeight: 16 },
  footer: { gap: 9, paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 }, disputesButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.border}`, justifyContent: "center", minHeight: 51 }, disputesText: { alignSelf: "stretch", color: colors.ink, ...bangers(21), lineHeight: 29, paddingTop: 4, textAlign: "center" }, actionRow: { flexDirection: "row", gap: 7 }, reportButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, flex: 1, justifyContent: "center", minHeight: 50, paddingHorizontal: 5 }, abandonButton: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1.1, justifyContent: "center", minHeight: 50, paddingHorizontal: 5 }, closeButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, flex: 1.45, justifyContent: "center", minHeight: 50, paddingHorizontal: 6 }, reportText: { color: colors.white, ...bangers(15), textAlign: "center" }, abandonText: { color: colors.ink, ...bangers(15), textAlign: "center" }, closeText: { alignSelf: "stretch", color: colors.ink, ...bangers(15), textAlign: "center" }, loading: { alignItems: "center", gap: 13, marginTop: 80 }, loadingText: { color: colors.text, ...bangers(24) }, errorState: { alignItems: "center", gap: 16, marginTop: 70 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 20, textAlign: "center" }, pressed: { opacity: 0.78, transform: [{ scale: 0.97 }] },
});
