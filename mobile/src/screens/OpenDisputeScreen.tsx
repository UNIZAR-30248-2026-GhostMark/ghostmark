import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { ApiError } from "../api/client";
import { openDispute } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "OpenDispute">;

export function OpenDisputeScreen({ navigation, route }: Props) {
  const [text, setText] = useState(""); const [error, setError] = useState<string>(); const [sending, setSending] = useState(false);
  async function submit() {
    const trimmed = text.trim();
    if (!trimmed) { setError("Cuenta qué ocurrió para abrir la disputa."); return; }
    setSending(true); setError(undefined);
    try {
      const dispute = await openDispute(route.params.gameId, route.params.tipo ?? "ELIMINACION", trimmed);
      navigation.replace("Dispute", { gameId: route.params.gameId, disputeId: dispute.id, returnToCard: route.params.returnToCard });
    } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : "No hemos podido abrir la disputa. Inténtalo de nuevo."); } finally { setSending(false); }
  }
  const negative = route.params.tipo === "NEGATIVA";
  return <Screen><View style={styles.screen}><Pressable accessibilityLabel="Volver" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.back}><Text style={styles.backText}>←</Text></Pressable><Text style={styles.title}>{negative ? "NO ME ENSEÑA EL CÓDIGO" : "ABRIR DISPUTA"}</Text><Text style={styles.body}>{negative ? "Explica cómo se negó a enseñarte su código." : "Explica qué pasó. La otra persona podrá responder y el árbitro decidirá."}</Text><TextInput accessibilityLabel="Tu versión de los hechos" autoFocus maxLength={280} multiline onChangeText={setText} placeholder={negative ? "Le pedí el código y se negó a enseñármelo." : "No me pidió un boli, me pidió un lápiz."} placeholderTextColor={colors.textMuted} style={styles.input} textAlignVertical="top" value={text} /><Text style={styles.counter}>{text.length}/280</Text>{error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}<View style={styles.action}><PrimaryButton disabled={sending} onPress={() => void submit()}>{sending ? "ABRIENDO…" : "ENVIAR DISPUTA"}</PrimaryButton></View></View></Screen>;
}
const styles = StyleSheet.create({ screen: { flex: 1 }, back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, height: 45, justifyContent: "center", width: 45 }, backText: { color: colors.text, fontSize: 30, lineHeight: 32 }, title: { color: colors.text, ...bangers(37), lineHeight: 43, marginTop: 29, textAlign: "center" }, body: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 15, lineHeight: 22, marginTop: 17, textAlign: "center" }, input: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 15, borderWidth: 3, color: colors.ink, fontFamily: "ZenMaru", fontSize: 16, height: 180, marginTop: 25, padding: 15 }, counter: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 12, marginTop: 6, textAlign: "right" }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 16, textAlign: "center" }, action: { marginTop: "auto", paddingBottom: 8 } });
