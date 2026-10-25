import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useMemo, useState } from "react";
import { Animated, Image, Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getMyMissionCard, type MissionCard } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "MissionCard">;

export function MissionCardScreen({ route }: Props) {
  const { gameId } = route.params;
  const [card, setCard] = useState<MissionCard>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const flip = useMemo(() => new Animated.Value(0), []);

  useEffect(() => {
    let active = true;
    getMyMissionCard(gameId).then((result) => {
      if (active) setCard(result);
    }).catch((requestError: unknown) => {
      if (!active) return;
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar tu carta. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [attempt, gameId]);

  function reveal(value: number) {
    Animated.timing(flip, { duration: 170, toValue: value, useNativeDriver: true }).start();
  }

  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.content}>
      <Tabs />
      <Text style={styles.objectiveLabel}>Tu objetivo</Text>
      {error ? <View style={styles.errorState}>
        <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text>
        <PrimaryButton onPress={() => { setError(undefined); setAttempt((value) => value + 1); }}>REINTENTAR</PrimaryButton>
      </View> : card ? <Pressable
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
    <View style={styles.footer}>
      <Pressable accessibilityRole="button" disabled style={styles.codeButton}><Text style={styles.codeText}>MI CÓDIGO</Text></Pressable>
      <Pressable accessibilityRole="button" disabled style={styles.removeButton}><Text style={styles.removeText}>ELIMINAR</Text></Pressable>
    </View>
  </View></Screen>;
}

function Tabs() {
  return <View style={styles.tabs}>
    <View style={styles.tabOn}><Text style={styles.tabOnText}>MISIÓN</Text></View>
    <View style={styles.tab}><Text style={styles.tabText}>Feed</Text></View>
    <View style={styles.tab}><Text style={styles.tabText}>Partida</Text></View>
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
    <Text adjustsFontSizeToFit minimumFontScale={0.58} numberOfLines={2} style={styles.targetName}>{card.aliasObjetivo.toUpperCase()}</Text>
    <View style={styles.ruleDivider} />
    <View style={styles.missionTag}><Text style={styles.missionTagText}>Misión</Text></View>
    <Text adjustsFontSizeToFit minimumFontScale={0.55} numberOfLines={5} style={styles.missionText}>{card.mision.toUpperCase()}</Text>
  </View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24, paddingTop: 18 },
  tabs: { backgroundColor: colors.ink, borderRadius: 15, flexDirection: "row", height: 49, padding: 4, width: "100%" },
  tab: { alignItems: "center", flex: 1, justifyContent: "center" },
  tabOn: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 11, borderWidth: 2, flex: 1, justifyContent: "center", shadowColor: colors.border, shadowOffset: { height: 3, width: 0 }, shadowOpacity: 1, shadowRadius: 0 },
  tabText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 14 },
  tabOnText: { color: colors.ink, fontFamily: "Bangers", fontSize: 19, letterSpacing: 0.5 },
  objectiveLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16, marginTop: 22 },
  card: { height: 392, marginTop: 12, position: "relative", transform: [{ rotate: "2deg" }], width: 286 },
  cardFace: { ...StyleSheet.absoluteFill, backfaceVisibility: "hidden", borderColor: colors.ink, borderRadius: 28, borderWidth: 4, overflow: "hidden", shadowColor: colors.ink, shadowOffset: { height: 8, width: 0 }, shadowOpacity: 0.6, shadowRadius: 0 },
  loadingCard: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 28, borderWidth: 3, height: 392, justifyContent: "center", marginTop: 12, width: 286 },
  loading: { color: colors.lime, fontFamily: "Bangers", fontSize: 25 },
  errorState: { alignItems: "center", flex: 1, gap: 20, justifyContent: "center", width: "100%" },
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
  targetName: { color: colors.ink, fontFamily: "Bangers", fontSize: 55, letterSpacing: 1, marginTop: 16, minHeight: 62 },
  ruleDivider: { backgroundColor: colors.ink, height: 3, marginTop: 10, opacity: 0.2, width: "100%" },
  missionTag: { alignSelf: "flex-start", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, marginTop: 17, paddingHorizontal: 12, paddingVertical: 5 },
  missionTagText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 },
  missionText: { color: colors.ink, flex: 1, fontFamily: "Bangers", fontSize: 31, letterSpacing: 0.4, lineHeight: 36, marginTop: 15 },
  footer: { gap: 13, paddingBottom: 10, paddingHorizontal: 24, paddingTop: 8 },
  codeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 15, borderWidth: 3, paddingVertical: 12, shadowColor: colors.ink, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 0.7, shadowRadius: 0 },
  codeText: { color: colors.text, fontFamily: "Bangers", fontSize: 21, letterSpacing: 0.7 },
  removeButton: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 15, borderWidth: 3, paddingVertical: 12, shadowColor: colors.border, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 0.7, shadowRadius: 0 },
  removeText: { color: colors.ink, fontFamily: "Bangers", fontSize: 21, letterSpacing: 0.7 },
});
