import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { ApiError } from "../api/client";
import { answerDispute, decideDispute, getDispute } from "../api/game";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { Dispute } from "../types/game";
import type { RootStackParamList } from "../types/navigation";
import { decisionLabels, disputeBubbles, disputeHeader, disputeOutcome, disputeWaiting } from "../utils/dispute";

type Props = NativeStackScreenProps<RootStackParamList, "Dispute">;
function remaining(until: string, now: number) {
  const seconds = Math.max(0, Math.ceil((Date.parse(until) - now) / 1000));
  return Math.floor(seconds / 3600) + " h " + String(Math.floor(seconds / 60) % 60).padStart(2, "0") + " min";
}
function zoneResult(result: Dispute["resultadoZona"]) { return result === "FUERA" ? "Fuera" : result === "DENTRO" ? "Dentro" : "No concluyente"; }

export function DisputeScreen({ navigation, route }: Props) {
  // Al abrir una disputa se reemplaza el formulario para que no vuelva a enviarse al pulsar Atrás.
  // El historial que queda debajo aún contiene «Te han cazado», por eso aquí se reinicia hacia la carta.
  const returnToCard = route.params.returnToCard;
  const leaveToCard = useCallback(() => {
    if (!returnToCard) { navigation.goBack(); return; }
    navigation.reset({ index: 0, routes: [{ name: "MissionCard", params: {
      gameId: route.params.gameId,
    } }] });
  }, [navigation, returnToCard, route.params.gameId]);
  const [dispute, setDispute] = useState<Dispute>();
  const [error, setError] = useState<string>(); const [answer, setAnswer] = useState(""); const [sending, setSending] = useState(false); const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    let active = true;
    getDispute(route.params.gameId, route.params.disputeId).then((value) => { if (active) setDispute(value); }).catch((reason: unknown) => { if (active) setError(reason instanceof ApiError ? reason.message : "No hemos podido cargar la disputa."); });
    return () => { active = false; };
  }, [route.params.disputeId, route.params.gameId]);
  useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 1_000); return () => clearInterval(timer); }, []);
  useEffect(() => navigation.addListener("beforeRemove", (event) => {
    if (!returnToCard) return;
    event.preventDefault();
    leaveToCard();
  }), [navigation, returnToCard, leaveToCard]); // También captura el botón físico Atrás de Android.
  async function respond() {
    if (!dispute || !answer.trim()) return;
    setSending(true);
    try { setDispute(await answerDispute(route.params.gameId, dispute.id, answer.trim())); setAnswer(""); }
    catch (reason) { setError(reason instanceof ApiError ? reason.message : "No hemos podido enviar tu respuesta."); } finally { setSending(false); }
  }
  async function decide(razon: "CAZADOR" | "VICTIMA") {
    if (!dispute) return;
    setSending(true);
    try { setDispute(await decideDispute(route.params.gameId, dispute.id, razon)); }
    catch (reason) { setError(reason instanceof ApiError ? reason.message : "No hemos podido resolver la disputa."); } finally { setSending(false); }
  }
  const open = dispute?.estado === "ABIERTA";
  const labels = dispute ? decisionLabels(dispute) : undefined;
  const waiting = dispute ? disputeWaiting(dispute) : null;
  const outcome = dispute ? disputeOutcome(dispute) : null;
  return <Screen padded={false}><KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"} style={styles.screen}>
    <View style={styles.header}><Pressable accessibilityLabel="Volver" accessibilityRole="button" onPress={leaveToCard} style={styles.back}><Ionicons color={colors.text} name="arrow-back" size={23} /></Pressable><Text style={styles.headerTitle}>DISPUTA</Text>{dispute && open ? <View style={styles.timer}><Text numberOfLines={1} style={styles.timerText}>⚖ Quedan {remaining(dispute.plazoHasta, now)}</Text></View> : null}</View>
    {error ? <View style={styles.loading}><Text style={styles.error}>{error}</Text></View> : !dispute ? <View style={styles.loading}><ActivityIndicator color={colors.lime} size="large" /></View> : <ScrollView automaticallyAdjustKeyboardInsets contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <View style={styles.mission}><Text adjustsFontSizeToFit minimumFontScale={0.55} numberOfLines={2} style={styles.missionTitle}>{dispute.mision.toUpperCase()}</Text><Text style={styles.meta}>{disputeHeader(dispute, new Date(now))}</Text></View>
      {disputeBubbles(dispute).map((bubble) => <Bubble align={bubble.align} author={bubble.author} key={bubble.align} text={bubble.text} />)}
      {dispute.puedoResponder ? <View style={styles.response}><TextInput accessibilityLabel="Tu respuesta" maxLength={280} multiline onChangeText={setAnswer} placeholder="Escribe tu versión…" placeholderTextColor={colors.textMuted} style={styles.responseInput} textAlignVertical="top" value={answer} /><Pressable accessibilityRole="button" disabled={sending || !answer.trim()} onPress={() => void respond()} style={[styles.reply, (!answer.trim() || sending) && styles.dim]}><Text style={styles.replyText}>RESPONDER</Text></Pressable></View> : null}
      {waiting ? <Text style={styles.waiting}>{waiting}</Text> : null}
      {dispute.soyArbitro && dispute.resultadoZona ? <View style={styles.zone}><Text style={styles.zoneIcon}>⌾</Text><Text style={styles.zoneText}>Resultado de zona</Text><Text style={styles.zoneResult}>{zoneResult(dispute.resultadoZona)}</Text></View> : null}
      {open && dispute.soyArbitro && labels ? <View style={styles.decisions}><Pressable accessibilityRole="button" disabled={sending} onPress={() => void decide("CAZADOR")} style={({ pressed }) => [styles.confirmDecision, (pressed || sending) && styles.dim]}><Text adjustsFontSizeToFit minimumFontScale={0.72} numberOfLines={1} style={styles.confirmDecisionText}>{labels.hunter}</Text></Pressable><Pressable accessibilityRole="button" disabled={sending} onPress={() => void decide("VICTIMA")} style={({ pressed }) => [styles.cancelDecision, (pressed || sending) && styles.dim]}><Text style={styles.cancelDecisionText}>{labels.victim}</Text></Pressable></View> : null}
      {outcome ? <><Text style={styles.resolved}>{outcome.title}</Text>{outcome.detail ? <Text style={styles.waiting}>{outcome.detail}</Text> : null}</> : null}
    </ScrollView>}
  </KeyboardAvoidingView></Screen>;
}
function Bubble({ align, author, text }: { align: "left" | "right"; author: string; text: string }) { return <View style={[styles.bubble, align === "right" && styles.bubbleRight]}><Text style={styles.bubbleAuthor}>{author}</Text><Text style={styles.bubbleText}>{text}</Text></View>; }
const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, header: { alignItems: "center", flexDirection: "row", gap: 11, paddingHorizontal: 24, paddingTop: 18 }, back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, height: 44, justifyContent: "center", width: 44 }, headerTitle: { color: colors.text, ...bangers(27), flex: 1, lineHeight: 36, paddingTop: 3 }, timer: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, maxWidth: 153, paddingHorizontal: 8, paddingVertical: 7 }, timerText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 10 }, content: { padding: 24, paddingBottom: 38, paddingTop: 17 }, loading: { alignItems: "center", flex: 1, justifyContent: "center", padding: 28 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", textAlign: "center" }, mission: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 17, borderWidth: 3, padding: 16 }, missionTitle: { color: colors.ink, ...bangers(24), lineHeight: 33, paddingTop: 3 }, meta: { color: colors.surfaceRaised, fontFamily: "ZenMaru", fontSize: 11, marginTop: 8 }, bubble: { alignSelf: "flex-start", backgroundColor: "#D7C6FF", borderColor: colors.ink, borderRadius: 14, borderWidth: 3, marginTop: 32, maxWidth: "78%", padding: 12 }, bubbleRight: { alignSelf: "flex-end", backgroundColor: colors.white, marginTop: 15 }, bubbleAuthor: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 }, bubbleText: { color: colors.ink, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 18, marginTop: 6 }, response: { alignSelf: "stretch", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, marginTop: 18, padding: 10 }, responseInput: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaru", fontSize: 13, minHeight: 100, padding: 10 }, reply: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, justifyContent: "center", marginTop: 9, minHeight: 42 }, replyText: { color: colors.ink, ...bangers(16), lineHeight: 25, paddingTop: 3 }, waiting: { color: colors.textMuted, fontFamily: "ZenMaruBold", marginTop: 20, textAlign: "center" }, zone: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", gap: 9, marginTop: 24, minHeight: 58, paddingHorizontal: 12 }, zoneIcon: { color: colors.lime, fontSize: 23 }, zoneText: { color: colors.text, flex: 1, fontFamily: "ZenMaruBold", fontSize: 12 }, zoneResult: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 7, borderWidth: 1, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11, paddingHorizontal: 9, paddingVertical: 4 }, decisions: { gap: 16, marginTop: 28 }, confirmDecision: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, boxShadow: "0px 5px 0px " + colors.border, justifyContent: "center", minHeight: 54, paddingHorizontal: 13 }, confirmDecisionText: { alignSelf: "stretch", color: colors.ink, ...bangers(20), lineHeight: 29, paddingTop: 4, textAlign: "center" }, cancelDecision: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, boxShadow: "0px 5px 0px " + colors.border, justifyContent: "center", minHeight: 54 }, cancelDecisionText: { alignSelf: "stretch", color: colors.ink, ...bangers(22), lineHeight: 30, paddingTop: 4, textAlign: "center" }, resolved: { color: colors.text, ...bangers(21), lineHeight: 30, marginTop: 27, paddingTop: 4, textAlign: "center" }, dim: { opacity: 0.5 },
});
