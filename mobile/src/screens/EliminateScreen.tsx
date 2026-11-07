import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { eliminateTarget } from "../api/game";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Eliminate">;
const keypad = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "←"];

export function EliminateScreen({ navigation, route }: Props) {
  const [code, setCode] = useState("");
  const [error, setError] = useState<string>();
  const [sending, setSending] = useState(false);
  const [attempts, setAttempts] = useState(3);
  const [blockedUntil, setBlockedUntil] = useState<string>();
  const [now, setNow] = useState(() => Date.now());
  const target = route.params.targetAlias?.toUpperCase() ?? "TU OBJETIVO";
  const blockedSeconds = blockedUntil
    ? Math.max(0, Math.ceil((Date.parse(blockedUntil) - now) / 1000))
    : 0;
  const blocked = blockedSeconds > 0;

  useEffect(() => {
    if (!blockedUntil) return undefined;
    const timer = setInterval(() => {
      const currentNow = Date.now();
      setNow(currentNow);
      if (Date.parse(blockedUntil) <= currentNow) {
        setBlockedUntil(undefined);
        setError(undefined);
        setAttempts(3);
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [blockedUntil]);

  async function submit(nextCode: string) {
    if (nextCode.length !== 6 || sending) return;
    setSending(true);
    try {
      const result = await eliminateTarget(route.params.gameId, nextCode);
      navigation.replace("EliminationSuccess", { gameId: route.params.gameId, result });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        const remaining = requestError.details.intentosRestantes;
        if (typeof remaining === "number") setAttempts(remaining);
        const until = requestError.details.bloqueadoHasta;
        if (requestError.code === "INTENTOS_BLOQUEADOS" && typeof until === "string") {
          setBlockedUntil(until);
          setNow(() => Date.now());
          setAttempts(3);
        }
        setError(requestError.message);
      } else {
        setError("No hemos podido comprobar el código. Inténtalo de nuevo.");
      }
      setCode("");
    } finally {
      setSending(false);
    }
  }

  function pressKey(key: string) {
    if (sending || blocked) return;
    setError(undefined);
    if (key === "←") {
      setCode(code.slice(0, -1));
      return;
    }
    if (!key || code.length >= 6) return;
    // El envío va fuera del cambio de estado: React puede repetir esa función y mandarlo dos veces.
    const next = `${code}${key}`;
    setCode(next);
    if (next.length === 6) void submit(next);
  }

  return <View style={styles.screen}>
    <View style={styles.header}><Pressable accessibilityLabel="Volver a mi carta" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.backButton}><Ionicons color={colors.text} name="arrow-back" size={23} /></Pressable><Text style={styles.headerTitle}>ELIMINAR</Text></View>
    <View style={styles.content}>
      <Text adjustsFontSizeToFit minimumFontScale={0.55} numberOfLines={2} style={styles.title}>CÓDIGO DE {target}</Text>
      <Text style={styles.description}>Pídele que te enseñe su código</Text>
      <View accessibilityLabel={`Código introducido: ${code.length} de 6 dígitos`} style={styles.codeSlots}>{Array.from({ length: 6 }, (_, index) => <View key={index} style={[styles.slot, index === code.length && !sending && !blocked && styles.slotCurrent]}><Text style={styles.slotText}>{code[index] ?? ""}</Text></View>)}</View>
      <View accessibilityLiveRegion="polite" style={styles.feedback}>
        {blocked ? <><Text style={styles.blockedTitle}>INTENTOS BLOQUEADOS</Text><Text style={styles.blockedMessage}>Podrás volver a intentarlo en {blockedSeconds} s</Text></> : <>{error ? <Text style={styles.error}>{error}</Text> : null}<Text style={styles.attempts}>{sending ? "COMPROBANDO…" : `Te quedan ${attempts} ${attempts === 1 ? "intento" : "intentos"}`}</Text></>}
      </View>
      <View style={styles.keypad}>{keypad.map((key, index) => key ? <Pressable accessibilityLabel={key === "←" ? "Borrar último dígito" : `Introducir ${key}`} accessibilityRole="button" disabled={sending || blocked} key={key} onPress={() => pressKey(key)} style={({ pressed }) => [styles.key, pressed && !blocked && styles.pressed, (sending || blocked) && styles.disabled]}><Text style={styles.keyText}>{key === "←" ? "⌫" : key}</Text></Pressable> : <View key={`empty-${index}`} style={styles.keyPlaceholder} />)}</View>
    </View>
  </View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  header: { alignItems: "center", flexDirection: "row", gap: 12, paddingHorizontal: 33, paddingTop: 55 },
  backButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, boxShadow: `0px 3px 0px ${colors.ink}`, height: 45, justifyContent: "center", width: 45 },
  headerTitle: { color: colors.white, ...bangers(26), letterSpacing: 0.7 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 33 },
  title: { alignSelf: "stretch", color: colors.white, ...bangers(40), lineHeight: 44, marginTop: 24, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 },
  description: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 14, marginTop: 5, textAlign: "center" },
  codeSlots: { flexDirection: "row", gap: 10, marginTop: 27 },
  slot: { alignItems: "center", backgroundColor: "#F9F5FF", borderColor: colors.ink, borderRadius: 12, borderWidth: 3, boxShadow: `0px 3px 0px ${colors.border}`, height: 57, justifyContent: "center", width: 43 },
  slotCurrent: { backgroundColor: colors.lime }, slotText: { color: colors.ink, ...bangers(34), lineHeight: 40 },
  feedback: { alignItems: "center", marginTop: 19, minHeight: 44 },
  attempts: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 5, textAlign: "center" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, marginHorizontal: 15, textAlign: "center" },
  blockedTitle: { color: colors.danger, ...bangers(19), lineHeight: 24, textAlign: "center" },
  blockedMessage: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 4, textAlign: "center" },
  keypad: { flexDirection: "row", flexWrap: "wrap", justifyContent: "space-between", marginTop: "auto", paddingBottom: 41, rowGap: 11, width: "100%" },
  key: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 14, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.ink}`, height: 61, justifyContent: "center", width: "30%" },
  keyPlaceholder: { height: 61, width: "30%" },
  keyText: { alignSelf: "stretch", color: colors.white, ...bangers(27), lineHeight: 34, paddingBottom: 1, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 },
  disabled: { opacity: 0.55 }, pressed: { opacity: 0.78, transform: [{ scale: 0.98 }] },
});
