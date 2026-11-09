import { useFocusEffect } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Animated, Image, Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getMyGames, getMyMissionCard, type MissionCard } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { RootStackParamList } from "../types/navigation";
import { hasSeenCaught } from "../utils/caughtSeen";

type Props = NativeStackScreenProps<RootStackParamList, "MissionCard">;

export function MissionCardScreen({ navigation, route }: Props) {
  const { gameId, revealOnce } = route.params;
  const [card, setCard] = useState<MissionCard>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const flip = useMemo(() => new Animated.Value(0), []);

  const loadCard = useCallback(() => {
    let active = true;
    getMyMissionCard(gameId).then(async (result) => {
      // El eliminado ve «Te han cazado» la primera vez que abre la carta (R11-25).
      if (result.estado === "ELIMINADO" && !(await hasSeenCaught(gameId))) {
        if (active) navigation.navigate("Caught", { gameId });
      }
      if (active) setCard(result);
    }).catch(async (requestError: unknown) => {
      if (!active) return;
      // Si la partida se ha cerrado mientras tanto, se lleva al jugador a la pantalla de fin.
      if (requestError instanceof ApiError && requestError.code === "PARTIDA_NO_EN_JUEGO") {
        const closed = await getMyGames().then((games) => games.find((game) => game.id === gameId && game.estado === "CERRADA")).catch(() => undefined);
        if (!active) return;
        if (closed) { navigation.replace("GameEnded", { game: closed }); return; }
      }
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar tu carta. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [gameId, navigation]);

  // Se recarga al volver a la carta, por ejemplo tras eliminar a tu objetivo.
  useFocusEffect(loadCard);
  useEffect(() => (attempt > 0 ? loadCard() : undefined), [attempt, loadCard]);

  function reveal(value: number) {
    Animated.timing(flip, { duration: 170, toValue: value, useNativeDriver: true }).start();
  }

  // Tras un acierto con herencia, la carta nueva gira sola una vez (UI11). Se espera a la carta
  // recargada al volver: la que había en pantalla es la del objetivo anterior.
  const pendingReveal = useRef(false);
  const flipBack = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => {
    if (!revealOnce) return;
    pendingReveal.current = true;
    navigation.setParams({ revealOnce: false });
  }, [navigation, revealOnce]);
  useEffect(() => {
    if (!pendingReveal.current || !card) return;
    pendingReveal.current = false;
    if (card.estado !== "VIVO" || isLastAlive(card)) return;
    reveal(1);
    flipBack.current = setTimeout(() => reveal(0), 1600);
  }, [card]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => () => clearTimeout(flipBack.current), []);

  const spectator = card?.estado === "ELIMINADO";
  const lastAlive = card ? isLastAlive(card) : false;

  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.content}>
      <Tabs gameId={gameId} navigation={navigation} spectator={spectator} />
      {spectator ? null : <Text style={styles.objectiveLabel}>Tu objetivo</Text>}
      {error ? <View style={styles.errorState}>
        <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text>
        <PrimaryButton onPress={() => { setError(undefined); setAttempt((value) => value + 1); }}>REINTENTAR</PrimaryButton>
      </View> : card && spectator ? <SpectatorCard /> : card && lastAlive ? <LastAliveCard /> : card ? <Pressable
        accessibilityHint="Mantén pulsada la carta para ver el objetivo y la misión"
        accessibilityLabel="Carta de misión"
        accessibilityRole="button"
        onPressIn={() => reveal(1)}
        onPressOut={() => reveal(0)}
        style={styles.card}
      >
        <Animated.View style={[styles.cardFace, { opacity: flip.interpolate({ inputRange: [0, 0.42, 0.58, 1], outputRange: [1, 0, 0, 0] }), transform: [{ perspective: 850 }, { rotateY: flip.interpolate({ inputRange: [0, 1], outputRange: ["0deg", "90deg"] }) }] }]}>
          <HiddenCard />
        </Animated.View>
        <Animated.View style={[styles.cardFace, { opacity: flip.interpolate({ inputRange: [0, 0.42, 0.58, 1], outputRange: [0, 0, 0, 1] }), transform: [{ perspective: 850 }, { rotateY: flip.interpolate({ inputRange: [0, 1], outputRange: ["-90deg", "0deg"] }) }] }]}>
          <RevealedCard card={card} />
        </Animated.View>
      </Pressable> : <View style={styles.loadingCard}><Text style={styles.loading}>CARGANDO TU CARTA…</Text></View>}
    </View>
    {/* El espectador ya no tiene código ni objetivo; el último vivo, nadie a quien eliminar (R11-26). */}
    {spectator ? null : <View style={styles.footer}>
      <Pressable accessibilityRole="button" onPress={() => navigation.navigate("LifeCode", { gameId })} style={styles.codeButton}><Text style={styles.codeText}>MI CÓDIGO</Text></Pressable>
      {lastAlive ? null : <Pressable accessibilityRole="button" onPress={() => navigation.navigate("Eliminate", { gameId, targetAlias: card?.aliasObjetivo })} style={styles.removeButton}><Text style={styles.removeText}>ELIMINAR</Text></Pressable>}
    </View>}
  </View></Screen>;
}

// Sin objetivo ni misión y aún vivo: no queda nadie más en juego.
function isLastAlive(card: MissionCard) {
  return card.estado === "VIVO" && card.aliasObjetivo === null && card.mision === null;
}

function Tabs({ gameId, navigation, spectator }: { gameId: string; navigation: Props["navigation"]; spectator: boolean }) {
  return <View style={styles.tabs}>
    <View style={[styles.tabOn, spectator && styles.tabOnSpectator]}><Text style={styles.tabOnText}>MISIÓN</Text></View>
    <Pressable accessibilityLabel="Ir al feed" accessibilityRole="button" onPress={() => navigation.navigate("Feed", { gameId, isPlayer: true })} style={styles.tab}><Text style={styles.tabText}>Feed</Text></Pressable>
    <Pressable accessibilityLabel="Ir a la pestaña Partida" accessibilityRole="button" onPress={() => navigation.navigate("GameTab", { gameId })} style={styles.tab}><Text style={styles.tabText}>Partida</Text></Pressable>
  </View>;
}

const OUTLINE = [[-3, 0], [3, 0], [0, -3], [0, 3], [-2, -2], [2, -2], [-2, 2], [2, 2], [0, 5]] as const;

function SpectatorCard() {
  return <>
    <View accessibilityLabel="Eres espectador" style={[styles.card, styles.spectatorCard]}>
      <Image source={require("../../assets/ghosts/fantasma-ko-gris.png")} style={styles.spectatorGhost} />
      <View style={styles.spectatorTitleWrap}>
        {OUTLINE.map(([x, y]) => <Text accessibilityElementsHidden aria-hidden importantForAccessibility="no-hide-descendants" key={`${x},${y}`} style={[styles.spectatorTitle, styles.spectatorOutline, { transform: [{ translateX: x }, { translateY: y }] }]}>ERES ESPECTADOR</Text>)}
        <Text style={styles.spectatorTitle}>ERES ESPECTADOR</Text>
      </View>
    </View>
    <Text style={styles.spectatorHint}>Sigue la partida en Feed y en Partida hasta el final.</Text>
  </>;
}

function LastAliveCard() {
  return <View style={[styles.card, styles.cardFace, styles.cardFront, styles.lastAliveCard]}>
    <View style={styles.objectiveTag}><Text style={styles.objectiveTagText}>Objetivo</Text></View>
    <Text style={styles.lastAliveText}>Ya no tienes objetivo: eres la última persona en juego</Text>
  </View>;
}

function HiddenCard() {
  return <View style={styles.cardBack}>
    <View style={styles.innerFrame}>
      <View pointerEvents="none" style={styles.rays}>{Array.from({ length: 18 }, (_, index) => <View key={index} style={[styles.ray, { transform: [{ rotate: `${index * 20}deg` }] }]} />)}</View>
      <View style={styles.targetCircle} />
      <Image accessibilityLabel="Carta boca abajo" source={require("../../assets/ghosts/fantasma-picaro.png")} style={styles.ghost} />
      <View style={styles.holdPrompt}><Text style={styles.holdText}>Mantén pulsado para ver</Text></View>
    </View>
  </View>;
}

function RevealedCard({ card }: { card: MissionCard }) {
  return <View style={styles.cardFront}>
    <View style={styles.objectiveTag}><Text style={styles.objectiveTagText}>Objetivo</Text></View>
    <Text adjustsFontSizeToFit minimumFontScale={0.4} numberOfLines={1} style={styles.targetName}>{card.aliasObjetivo?.toUpperCase() ?? "CUENTA ELIMINADA"}</Text>
    <View style={styles.ruleDivider} />
    <View style={styles.missionTag}><Text style={styles.missionTagText}>Misión</Text></View>
    <Text adjustsFontSizeToFit minimumFontScale={0.35} numberOfLines={5} style={styles.missionText}>{card.mision?.toUpperCase() ?? "SIN MISIÓN"}</Text>
  </View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24, paddingTop: 18 },
  tabs: { backgroundColor: colors.ink, borderRadius: 15, flexDirection: "row", height: 49, padding: 4, width: "100%" },
  tab: { alignItems: "center", flex: 1, justifyContent: "center" },
  tabOn: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 11, borderWidth: 2, boxShadow: `0px 3px 0px ${colors.border}`, flex: 1, justifyContent: "center" },
  tabText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 14 },
  tabOnSpectator: { backgroundColor: "#8E80AE" },
  tabOnText: { alignSelf: "stretch", color: colors.ink, ...bangers(19), letterSpacing: 0.5, textAlign: "center" },
  objectiveLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16, marginTop: 22 },
  card: { height: 392, marginTop: 12, position: "relative", transform: [{ rotate: "2deg" }], width: 286 },
  cardFace: { ...StyleSheet.absoluteFill, backfaceVisibility: "hidden", borderColor: colors.ink, borderRadius: 28, borderWidth: 4, boxShadow: `0px 8px 0px ${colors.ink}`, overflow: "hidden" },
  spectatorCard: { backgroundColor: "#4A3A66", borderColor: colors.ink, borderRadius: 26, borderWidth: 3, boxShadow: "0px 8px 16px -6px rgba(8, 2, 16, 0.75)", marginTop: 34, overflow: "hidden", transform: [{ rotate: "1deg" }] },
  spectatorGhost: { height: 200, left: 24, opacity: 0.55, position: "absolute", resizeMode: "contain", top: 40, width: 204 },
  spectatorTitleWrap: { bottom: 32, left: 0, position: "absolute", right: 0 },
  spectatorTitle: { color: "#C9BEE3", ...bangers(36), textAlign: "center" },
  spectatorOutline: { color: colors.ink, left: 0, position: "absolute", right: 0, top: 0 },
  spectatorHint: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 16, lineHeight: 22, marginTop: 28, paddingHorizontal: 12, textAlign: "center" },
  lastAliveCard: { position: "relative" },
  lastAliveText: { color: colors.ink, ...bangers(34), letterSpacing: 0.4, lineHeight: 38, marginTop: 24 },
  loadingCard: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 28, borderWidth: 3, height: 392, justifyContent: "center", marginTop: 12, width: 286 },
  loading: { color: colors.lime, ...bangers(25) },
  errorState: { flex: 1, gap: 20, justifyContent: "center", width: "100%" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 21, textAlign: "center" },
  cardBack: { backgroundColor: colors.white, flex: 1, padding: 10 },
  innerFrame: { alignItems: "center", backgroundColor: "#7E3BC1", borderColor: "#E7DFFF", borderRadius: 22, borderWidth: 2, flex: 1, justifyContent: "center", overflow: "hidden" },
  rays: { ...StyleSheet.absoluteFill, alignItems: "center", justifyContent: "center" },
  ray: { backgroundColor: "rgba(230, 215, 255, 0.35)", height: 520, position: "absolute", width: 7 },
  targetCircle: { backgroundColor: "#F35A65", borderColor: colors.ink, borderRadius: 999, borderWidth: 3, height: 180, position: "absolute", width: 180 },
  ghost: { height: 190, resizeMode: "contain", transform: [{ translateY: -5 }], width: 182, zIndex: 2 },
  holdPrompt: { backgroundColor: colors.ink, borderRadius: 14, bottom: 18, paddingHorizontal: 18, paddingVertical: 10, position: "absolute" },
  holdText: { color: colors.white, fontFamily: "ZenMaruBold", fontSize: 12 },
  cardFront: { backgroundColor: colors.white, flex: 1, paddingHorizontal: 20, paddingTop: 22 },
  objectiveTag: { alignSelf: "flex-start", backgroundColor: colors.textMuted, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, paddingHorizontal: 12, paddingVertical: 5 },
  objectiveTagText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 },
  targetName: { color: colors.ink, ...bangers(55), height: 62, letterSpacing: 1, marginTop: 16, width: "100%" },
  ruleDivider: { backgroundColor: colors.ink, height: 3, marginTop: 10, opacity: 0.2, width: "100%" },
  missionTag: { alignSelf: "flex-start", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, marginTop: 17, paddingHorizontal: 12, paddingVertical: 5 },
  missionTagText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 },
  missionText: { color: colors.ink, flex: 1, ...bangers(31), letterSpacing: 0.4, lineHeight: 34, marginTop: 12 },
  footer: { gap: 13, paddingBottom: 10, paddingHorizontal: 24, paddingTop: 8 },
  codeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 15, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.ink}`, paddingVertical: 12 },
  codeText: { alignSelf: "stretch", color: colors.text, ...bangers(21), letterSpacing: 0.7, textAlign: "center" },
  removeButton: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 15, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.border}`, paddingVertical: 12 },
  removeText: { alignSelf: "stretch", color: colors.ink, ...bangers(21), letterSpacing: 0.7, textAlign: "center" },
});
