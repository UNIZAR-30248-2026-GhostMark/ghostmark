import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Animated, PanResponder, Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { requestAiMissionBatch, reviewAiMission } from "../api/game";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { AiMission, MissionBatch } from "../types/game";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "ReviewAiMissions">;

export function ReviewAiMissionsScreen({ navigation, route }: Props) {
  const [batch, setBatch] = useState<MissionBatch>(route.params.batch);
  const [missions, setMissions] = useState<AiMission[]>(route.params.batch.misiones);
  const [reviewed, setReviewed] = useState(0);
  const [total, setTotal] = useState(route.params.batch.misiones.length);
  const [approved, setApproved] = useState(0);
  const [error, setError] = useState<string>();
  const [libraryFallback, setLibraryFallback] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [translateX] = useState(() => new Animated.Value(0));
  const [cardScale] = useState(() => new Animated.Value(1));
  const [finishedOpacity] = useState(() => new Animated.Value(1));
  const [feedback, setFeedback] = useState<"APROBADA" | "RECHAZADA">();
  const current = missions[0];

  function animateDecision(state: "APROBADA" | "RECHAZADA") {
    setFeedback(state);
    const direction = state === "APROBADA" ? 1 : -1;
    const isLastMission = missions.length === 1;
    Animated.parallel([
      Animated.timing(translateX, { duration: 500, toValue: direction * 420, useNativeDriver: true }),
      Animated.timing(cardScale, { duration: 420, toValue: 0.93, useNativeDriver: true }),
    ]).start(() => {
      setMissions((items) => items.slice(1));
      setReviewed((value) => value + 1);
      if (state === "APROBADA") setApproved((value) => value + 1);
      setFeedback(undefined);
      if (!isLastMission) {
        // Espera a que React monte la nueva misión antes de devolver la carta al centro.
        // Si se reinicia antes, durante un fotograma se ve el texto anterior en la nueva carta.
        requestAnimationFrame(() => {
          translateX.setValue(0);
          cardScale.setValue(1);
          setIsSaving(false);
        });
        return;
      }
      finishedOpacity.setValue(0);
      Animated.sequence([
        Animated.delay(120),
        Animated.timing(finishedOpacity, { duration: 280, toValue: 1, useNativeDriver: true }),
      ]).start(() => setIsSaving(false));
    });
  }

  async function review(state: "APROBADA" | "RECHAZADA") {
    if (!current || isSaving) return;
    setIsSaving(true);
    setError(undefined);
    try {
      await reviewAiMission(route.params.game.id, current.id, state);
      animateDecision(state);
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido guardar la decisión. Inténtalo de nuevo.");
      translateX.setValue(0); setIsSaving(false);
    }
  }

  const panHandlers = PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dx) > 8,
    onPanResponderMove: (_, gesture) => translateX.setValue(gesture.dx),
    onPanResponderRelease: (_, gesture) => {
      if (gesture.dx > 80) void review("APROBADA");
      else if (gesture.dx < -80) void review("RECHAZADA");
      else Animated.spring(translateX, { toValue: 0, useNativeDriver: true }).start();
    },
  });

  async function anotherBatch() {
    setError(undefined); setIsSaving(true);
    try {
      const next = await requestAiMissionBatch(route.params.game.id, route.params.contexto);
      translateX.setValue(0);
      cardScale.setValue(1);
      setFeedback(undefined);
      setLibraryFallback(false);
      setBatch(next); setMissions(next.misiones); setTotal(next.misiones.length); setReviewed(0); setApproved(0);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 503 && requestError.code === "IA_NO_DISPONIBLE") {
        setLibraryFallback(true);
      } else {
        setError(requestError instanceof ApiError ? requestError.message : "No hemos podido pedir otra tanda. Inténtalo de nuevo.");
      }
    } finally { setIsSaving(false); }
  }

  function finish() { navigation.navigate("ShareGame", { game: route.params.game }); }

  return <Screen><View style={styles.screen}>
    <View style={styles.header}><Pressable accessibilityLabel="Volver a Inicio" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.back}><Ionicons color={colors.text} name="arrow-back" size={23} /></Pressable>{current ? <View style={styles.counter}><Text style={styles.counterText}>{Math.min(reviewed + 1, total)} de {total}</Text></View> : null}</View>
    <Text style={styles.heading}>APRUEBA LAS MISIONES</Text>
    {current ? <>
      <View style={styles.deck}>
        <View style={[styles.backCard, styles.backCardPurple]} /><View style={[styles.backCard, styles.backCardRed]} />
        <Animated.View {...panHandlers.panHandlers} style={[styles.card, feedback === "APROBADA" && styles.cardApproved, feedback === "RECHAZADA" && styles.cardRejected, { transform: [{ rotate: translateX.interpolate({ inputRange: [-140, 0, 140], outputRange: ["-10deg", "0deg", "10deg"] }) }, { translateX }, { scale: cardScale }] }]}>
          <Text style={styles.tag}>Misión propuesta</Text><Text style={styles.missionText}>{current.texto}</Text>
        </Animated.View>
      </View>
      <View style={styles.actions}><DecisionButton accessibilityLabel="Rechazar misión" color={colors.danger} icon="×" label="Rechazar" onPress={() => void review("RECHAZADA")} /><DecisionButton accessibilityLabel="Aprobar misión" color={colors.lime} icon="✓" label="Aprobar" onPress={() => void review("APROBADA")} /></View>
      <Text style={styles.hint}>Desliza a la derecha para aprobar, a la izquierda para rechazar</Text>
    </> : <Animated.View style={[styles.finished, { opacity: finishedOpacity }]}>{total === 0 ? <Text style={styles.finishedText}>La IA no ha propuesto ninguna misión válida.</Text> : <><Text style={styles.finishedTitle}>TANDA REVISADA</Text><Text style={styles.finishedText}>{`Has aprobado ${approved} ${approved === 1 ? "misión" : "misiones"}. Puedes pedir otra tanda o continuar con la partida.`}</Text></>}{batch.tandasRestantes > 0 ? <CompactButton disabled={isSaving} label={isSaving ? "PIDIENDO…" : `PEDIR OTRA TANDA (${batch.tandasRestantes})`} onPress={anotherBatch} /> : null}<CompactButton label="CONTINUAR" onPress={finish} /></Animated.View>}
    {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
    {libraryFallback ? <Text accessibilityLiveRegion="polite" style={styles.finishedText}>La IA no responde: se usarán misiones de la biblioteca</Text> : null}
  </View></Screen>;
}

function DecisionButton({ accessibilityLabel, color, icon, label, onPress }: { accessibilityLabel: string; color: string; icon: string; label: string; onPress: () => void }) {
  return <View style={styles.decision}><Pressable accessibilityLabel={accessibilityLabel} accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.decisionButton, { backgroundColor: color }, pressed && styles.pressed]}><Text style={styles.decisionIcon}>{icon}</Text></Pressable><Text style={styles.decisionLabel}>{label}</Text></View>;
}

function CompactButton({ disabled = false, label, onPress }: { disabled?: boolean; label: string; onPress: () => void }) {
  return <Pressable accessibilityLabel={label} accessibilityRole="button" disabled={disabled} onPress={onPress} style={({ pressed }) => [styles.compactButton, (pressed || disabled) && styles.pressed]}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.compactButtonText}>{label}</Text></Pressable>;
}

const styles = StyleSheet.create({
  screen: { flex: 1 }, header: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" },
  back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, height: 44, justifyContent: "center", width: 44 },
  counter: { backgroundColor: "#D7C6FF", borderColor: colors.ink, borderRadius: 8, borderWidth: 2, paddingHorizontal: 12, paddingVertical: 6 }, counterText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 12 },
  heading: { color: colors.white, ...bangers(38), lineHeight: 42, marginTop: 20, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 },
  deck: { alignItems: "center", height: 330, justifyContent: "center", marginTop: 23 }, backCard: { borderColor: colors.ink, borderRadius: 26, borderWidth: 3, height: 290, position: "absolute", width: "83%" }, backCardPurple: { backgroundColor: "#803FC0", transform: [{ rotate: "-7deg" }, { translateX: -14 }] }, backCardRed: { backgroundColor: colors.danger, transform: [{ rotate: "8deg" }, { translateX: 14 }] },
  card: { backgroundColor: "#FAF7FF", borderColor: colors.ink, borderRadius: 26, borderWidth: 3, boxShadow: `0px 6px 0px ${colors.ink}`, height: 300, padding: 28, width: "84%" }, cardApproved: { borderColor: "#5B8B14", borderWidth: 6, boxShadow: "0px 6px 0px #36550C" }, cardRejected: { borderColor: "#C42C38", borderWidth: 6, boxShadow: "0px 6px 0px #7D1520" }, tag: { alignSelf: "flex-start", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 12, paddingHorizontal: 11, paddingVertical: 5 }, missionText: { color: colors.ink, ...bangers(36), lineHeight: 41, marginTop: 21 },
  actions: { flexDirection: "row", justifyContent: "space-evenly", marginTop: 0 }, decision: { alignItems: "center", gap: 9 }, decisionButton: { alignItems: "center", borderColor: colors.ink, borderRadius: 18, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.ink}`, height: 68, justifyContent: "center", width: 68 }, decisionIcon: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 44, lineHeight: 47 }, decisionLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 14 }, hint: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 20, marginTop: 29, paddingBottom: 20, textAlign: "center" },
  finished: { alignItems: "center", flex: 1, gap: 16, justifyContent: "center", paddingHorizontal: 12 }, finishedTitle: { color: colors.lime, ...bangers(37), textAlign: "center" }, finishedText: { color: colors.text, fontFamily: "ZenMaru", fontSize: 15, lineHeight: 22, textAlign: "center" }, compactButton: { alignItems: "center", alignSelf: "stretch", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 13, borderWidth: 2, boxShadow: `0px 5px 0px ${colors.border}`, minHeight: 56, justifyContent: "center", paddingHorizontal: 18 }, compactButtonText: { alignSelf: "stretch", color: colors.ink, ...bangers(18), lineHeight: 24, textAlign: "center" }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, marginTop: 10, textAlign: "center" }, pressed: { opacity: 0.78, transform: [{ scale: 0.97 }] },
});
