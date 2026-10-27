import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { getMyGames } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { GameSummary } from "../types/game";
import type { RootStackParamList } from "../types/navigation";
import { formatGameDate } from "../utils/gameDate";
import { serverWakeupMessage, useServerWakeupNotice } from "../utils/serverWakeup";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;
const stateLabels: Record<GameSummary["estado"], string> = {
  EN_INSCRIPCION: "Inscripción abierta",
  EN_JUEGO: "En juego",
  CANCELADA: "Cancelada",
  CERRADA: "Cerrada",
};

export function HomeScreen({ navigation, route }: Props) {
  const [games, setGames] = useState<GameSummary[]>(route.params?.createdGame ? [route.params.createdGame] : []);
  const [gamesState, setGamesState] = useState<"loading" | "error" | "loaded">(route.params?.createdGame ? "loaded" : "loading");
  const [attempt, setAttempt] = useState(0);
  const featuredGame = games[0];
  const otherGames = games.slice(1);
  const showServerWakeupNotice = useServerWakeupNotice(gamesState === "loading", attempt);

  useEffect(() => {
    let mounted = true;
    void getMyGames().then((saved) => {
      if (mounted) {
        setGames(saved);
        setGamesState("loaded");
      }
    }).catch(() => {
      if (mounted && !route.params?.createdGame) setGamesState("error");
    });
    return () => { mounted = false; };
  }, [attempt, route.params?.createdGame]);

  function retryGames() {
    setGamesState("loading");
    setAttempt((value) => value + 1);
  }

  return <Screen padded={false}><View style={styles.screen}><View style={styles.content}><ScrollView contentContainerStyle={styles.gamesContent} showsVerticalScrollIndicator={false}><Header />{gamesState === "loading" ? <LoadingGames showServerWakeupNotice={showServerWakeupNotice} /> : gamesState === "error" ? <GamesLoadError onRetry={retryGames} /> : featuredGame ? <><FeaturedGame game={featuredGame} navigation={navigation} /><Text style={styles.otherTitle}>TUS OTRAS PARTIDAS</Text>{otherGames.length ? otherGames.map((game) => <OtherGame game={game} key={game.id} navigation={navigation} />) : <Text style={styles.otherEmpty}>Todavía no tienes otras partidas.</Text>}</> : <EmptyGames />}</ScrollView><View style={styles.actions}><Pressable accessibilityRole="button" onPress={() => navigation.navigate("CreateGameData")} style={({ pressed }) => [styles.createButton, pressed && styles.pressed]}><Text style={styles.createText}>CREAR PARTIDA</Text></Pressable><Pressable accessibilityRole="button" onPress={() => navigation.navigate("ScanInvitation")} style={({ pressed }) => [styles.joinButton, pressed && styles.pressed]}><Text style={styles.joinText}>UNIRSE</Text></Pressable></View></View><BottomBar navigation={navigation} /></View></Screen>;
}

function Header() { return <View style={styles.header}><View style={styles.brand}><Image accessibilityLabel="GhostMark" source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.brandGhost} /><Text style={styles.brandText}>GHOSTMARK</Text></View><View style={styles.notification}><Ionicons color={colors.text} name="notifications-outline" size={22} /></View></View>; }
function openGame(navigation: Props["navigation"], game: GameSummary) {
  if (game.estado === "EN_JUEGO") navigation.navigate("GameStarted", { game });
  else if (game.estado === "CANCELADA") navigation.navigate("GameCancelled", { game });
  else navigation.navigate("WaitingRoom", { gameId: game.id, game });
}
function FeaturedGame({ game, navigation }: { game: GameSummary; navigation: Props["navigation"] }) { return <Pressable accessibilityRole="button" onPress={() => openGame(navigation, game)} style={({ pressed }) => [styles.featured, pressed && styles.pressed]}><View style={styles.state}><Text style={styles.stateText}>{stateLabels[game.estado]}</Text></View><Image accessibilityLabel="Fantasma de la partida" source={require("../../assets/ghosts/fantasma-picaro.png")} style={styles.featuredGhost} /><Text style={styles.featuredTitle}>{game.nombre.toUpperCase()}</Text><Text style={styles.ends}>Hasta el {formatGameDate(game.finEn)}</Text>{game.estado === "EN_JUEGO" ? <View style={styles.missionButton}><Text style={styles.missionText}>VER MISIÓN</Text></View> : null}</Pressable>; }
function OtherGame({ game, navigation }: { game: GameSummary; navigation: Props["navigation"] }) { return <Pressable accessibilityRole="button" onPress={() => openGame(navigation, game)} style={({ pressed }) => [styles.otherCard, pressed && styles.pressed]}><View><Text style={styles.otherName}>{game.nombre}</Text><Text style={styles.otherInfo}>{game.estado === "EN_INSCRIPCION" ? `Inscripción · empieza ${formatGameDate(game.inicioEn)}` : stateLabels[game.estado]}</Text></View><Ionicons color={colors.ink} name="chevron-forward" size={24} /></Pressable>; }
function EmptyGames() { return <View style={styles.emptyCard}><Image accessibilityLabel="Fantasma esperando partidas" source={require("../../assets/ghosts/fantasma-pensativo.png")} style={styles.emptyGhost} /><Text style={styles.emptyTitle}>TODAVÍA NO HAY PARTIDAS</Text><Text style={styles.emptyText}>Cuando crees una partida o te unas a una, aparecerá aquí.</Text></View>; }
function LoadingGames({ showServerWakeupNotice }: { showServerWakeupNotice: boolean }) { return <View style={styles.loadingCard}><ActivityIndicator accessibilityLabel="Cargando partidas" color={colors.lime} size="large" /><Text style={styles.loadingText}>CARGANDO TUS PARTIDAS…</Text>{showServerWakeupNotice ? <Text accessibilityLiveRegion="polite" style={styles.loadingHint}>{serverWakeupMessage}</Text> : null}</View>; }
function GamesLoadError({ onRetry }: { onRetry: () => void }) { return <View style={styles.loadingCard}><Text accessibilityLiveRegion="polite" style={styles.errorText}>No hemos podido cargar tus partidas. Comprueba tu conexión.</Text><PrimaryButton onPress={onRetry}>REINTENTAR</PrimaryButton></View>; }
function BottomBar({ navigation }: Pick<Props, "navigation">) { return <View style={styles.bottomBar}><View style={[styles.navItem, styles.navActive]}><Ionicons color={colors.ink} name="game-controller-outline" size={19} /><Text style={[styles.navText, styles.navTextActive]}>Partidas</Text></View><View style={styles.navItem}><Ionicons color={colors.textMuted} name="people-outline" size={19} /><Text style={styles.navText}>Amigos</Text></View><Pressable accessibilityRole="button" onPress={() => navigation.navigate("Profile")} style={({ pressed }) => [styles.navItem, pressed && styles.pressed]}><Ionicons color={colors.textMuted} name="person-circle-outline" size={20} /><Text style={styles.navText}>Perfil</Text></Pressable></View>; }

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, content: { flex: 1 }, gamesContent: { paddingHorizontal: 24, paddingTop: 16 }, header: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" }, brand: { alignItems: "center", flexDirection: "row", gap: 8 }, brandGhost: { height: 38, resizeMode: "contain", width: 38 }, brandText: { color: colors.text, ...bangers(30), letterSpacing: 1 }, notification: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 2, height: 42, justifyContent: "center", position: "relative", width: 42 }, featured: { backgroundColor: "#7D34BF", borderColor: colors.border, borderRadius: 13, borderWidth: 3, height: 307, marginTop: 25, overflow: "hidden", padding: 16, position: "relative" }, state: { alignSelf: "flex-start", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, paddingHorizontal: 11, paddingVertical: 5, transform: [{ rotate: "-5deg" }] }, stateText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 }, featuredTitle: { color: colors.white, ...bangers(34), letterSpacing: 0.5, lineHeight: 36, marginTop: 20, maxWidth: "62%", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 }, featuredGhost: { height: 188, position: "absolute", resizeMode: "contain", right: -12, top: 20, width: 195 }, ends: { bottom: 78, color: colors.white, fontFamily: "ZenMaruBold", fontSize: 12, left: 16, position: "absolute" }, missionButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, bottom: 16, justifyContent: "center", left: 14, minHeight: 54, position: "absolute", right: 14, shadowColor: colors.ink, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 1, shadowRadius: 0 }, missionText: { color: colors.ink, ...bangers(22) }, otherTitle: { color: colors.text, ...bangers(27), marginTop: 24 }, otherEmpty: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, marginTop: 8 }, otherCard: { alignItems: "center", backgroundColor: "#D8C4FF", borderColor: colors.ink, borderRadius: 11, borderWidth: 2, flexDirection: "row", justifyContent: "space-between", marginTop: 10, paddingHorizontal: 13, paddingVertical: 11 }, otherName: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 14 }, otherInfo: { color: colors.ink, fontFamily: "ZenMaru", fontSize: 11, marginTop: 3 }, emptyCard: { alignItems: "center", backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 20, borderWidth: 3, marginTop: 38, paddingHorizontal: 22, paddingVertical: 24 }, emptyGhost: { height: 120, resizeMode: "contain", width: 145 }, emptyTitle: { color: colors.text, ...bangers(23), marginTop: 8, textAlign: "center" }, emptyText: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 14, lineHeight: 20, marginTop: 6, textAlign: "center" }, loadingCard: { alignItems: "center", backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 20, borderWidth: 3, gap: 14, marginTop: 38, paddingHorizontal: 22, paddingVertical: 38 }, loadingText: { color: colors.text, ...bangers(23), textAlign: "center" }, loadingHint: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 19, textAlign: "center" }, errorText: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 20, textAlign: "center" }, actions: { flexDirection: "row", gap: 10, marginHorizontal: 24, marginTop: 10, paddingBottom: 12 }, createButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 13, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 52 }, createText: { color: colors.ink, ...bangers(18) }, joinButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 52, opacity: 0.7 }, joinText: { color: colors.ink, ...bangers(20) }, bottomBar: { backgroundColor: colors.surface, flexDirection: "row", paddingBottom: 10, paddingHorizontal: 10, paddingTop: 9 }, navItem: { alignItems: "center", borderRadius: 14, flex: 1, flexDirection: "row", gap: 7, justifyContent: "center", minHeight: 46 }, navActive: { backgroundColor: colors.lime }, navText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 }, navTextActive: { color: colors.ink }, pressed: { opacity: 0.78, transform: [{ scale: 0.98 }] },
});
