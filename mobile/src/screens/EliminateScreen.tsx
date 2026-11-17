import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { eliminateTarget, getMyMissionCard } from "../api/game";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Eliminate">;
const keypad = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "←"];

export function EliminateScreen({ navigation, route }: Props) {
  const [code, setCode] = useState("");
  const [error, setError] = useState<string>();
  const [sending, setSending] = useState(false);
  // Los parámetros de navegación pueden pertenecer a una carta que React Navigation
  // conserva en memoria. No los mostramos como estado actual: solo vale la respuesta
  // recién recibida del servidor.
  const [attempts, setAttempts] = useState<number>();
  const [blockedUntil, setBlockedUntil] = useState<string | undefined>(route.params.blockedUntil ?? undefined);
  const [targetAlias, setTargetAlias] = useState(route.params.targetAlias);
  const [stateReady, setStateReady] = useState(false);
  const [stateAttempt, setStateAttempt] = useState(0);
  const [loadFailed, setLoadFailed] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const target = targetAlias?.toUpperCase() ?? "TU OBJETIVO";
  const blockedSeconds = blockedUntil
    ? Math.max(0, Math.ceil((Date.parse(blockedUntil) - now) / 1000))
    : 0;
  const blocked = blockedSeconds > 0;

  // La carta puede seguir recargándose al volver atrás. Antes de dejar escribir, se consulta
  // siempre el estado actual del servidor para no reutilizar intentos o bloqueos caducados.
  useEffect(() => {
    let active = true;
    getMyMissionCard(route.params.gameId).then((card) => {
      if (!active) return;
      if (card.estado === "ELIMINADO") {
        navigation.replace("Caught", { gameId: route.params.gameId });
        return;
      }
      if (typeof card.intentosRestantes !== "number") {
        setError("No hemos podido comprobar tus intentos. Inténtalo de nuevo.");
        setLoadFailed(true);
        return;
      }
      setAttempts(card.intentosRestantes);
      setBlockedUntil(card.bloqueadoHasta ?? undefined);
      setTargetAlias(card.aliasObjetivo);
      setError(undefined);
      setStateReady(true);
    }).catch((requestError: unknown) => {
      if (!active) return;
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido comprobar tus intentos. Inténtalo de nuevo.");
      setLoadFailed(true);
    });
    return () => { active = false; };
  }, [navigation, route.params.gameId, stateAttempt]);

  useEffect(() => {
    if (!blockedUntil) return undefined;
    const timer = setInterval(() => {
      const currentNow = Date.now();
      setNow(currentNow);
      if (Date.parse(blockedUntil) <= currentNow) {
        setBlockedUntil(undefined);
        setStateReady(false);
        setStateAttempt((value) => value + 1);
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [blockedUntil]);

  async function submit(nextCode: string) {
    if (nextCode.length !== 6 || sending || !stateReady) return;
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
          setStateReady(true);
        }
        setError(requestError.code === "HORARIO_LECTIVO"
          ? unavailableMessage(requestError.details, route.params.targetAlias)
          : requestError.message);
      } else {
        setError("No hemos podido comprobar el código. Inténtalo de nuevo.");
      }
      setCode("");
    } finally {
      setSending(false);
    }
  }

  // Si no se pudo consultar la carta (Render dormido, sin conexión), se vuelve a pedir.
  function retryState() {
    setError(undefined);
    setLoadFailed(false);
    setStateAttempt((value) => value + 1);
  }

  function pressKey(key: string) {
    if (sending || blocked || !stateReady) return;
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
        {blocked ? <><Text style={styles.blockedTitle}>INTENTOS BLOQUEADOS</Text><Text style={styles.blockedMessage}>Podrás volver a intentarlo en {blockedSeconds} s</Text></> : <>{error ? <Text style={styles.error}>{error}</Text> : null}{loadFailed ? <Pressable accessibilityRole="button" onPress={retryState} style={({ pressed }) => [styles.retryButton, pressed && styles.pressed]}><Text style={styles.retryText}>REINTENTAR</Text></Pressable> : <Text style={styles.attempts}>{sending ? "COMPROBANDO…" : !stateReady || attempts === undefined ? "COMPROBANDO INTENTOS…" : `Te ${attempts === 1 ? "queda 1 intento" : `quedan ${attempts} intentos`}`}</Text>}</>}
      </View>
      <View style={styles.keypad}>{keypad.map((key, index) => key ? <Pressable accessibilityLabel={key === "←" ? "Borrar último dígito" : `Introducir ${key}`} accessibilityRole="button" disabled={sending || blocked || !stateReady} key={key} onPress={() => pressKey(key)} style={({ pressed }) => [styles.key, pressed && !blocked && stateReady && styles.pressed, (sending || blocked || !stateReady) && styles.disabled]}><Text style={styles.keyText}>{key === "←" ? "⌫" : key}</Text></Pressable> : <View key={`empty-${index}`} style={styles.keyPlaceholder} />)}</View>
    </View>
  </View>;
}

function unavailableMessage(details: Readonly<Record<string, unknown>>, targetAlias?: string | null) {
  const who = details.motivo === "CAZADOR" ? "Estás" : `${targetAlias ?? "Tu objetivo"} está`;
  const availableAt = typeof details.disponibleEn === "string" ? Date.parse(details.disponibleEn) : Number.NaN;
  if (Number.isNaN(availableAt)) return `${who} en clase.`;
  const minutes = Math.max(1, Math.ceil((availableAt - Date.now()) / 60_000));
  const remaining = minutes >= 60
    ? `${Math.floor(minutes / 60)} h${minutes % 60 ? ` ${minutes % 60} min` : ""}`
    : `${minutes} min`;
  return `${who} en clase. Disponible en ${remaining}.`;
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
  slotCurrent: { backgroundColor: colors.lime }, slotText: { alignSelf: "stretch", color: colors.ink, ...bangers(34), lineHeight: 44, textAlign: "center" },
  feedback: { alignItems: "center", marginTop: 19, minHeight: 44 },
  attempts: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 5, textAlign: "center" },
  retryButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, boxShadow: `0px 3px 0px ${colors.border}`, justifyContent: "center", marginTop: 8, minHeight: 38, paddingHorizontal: 26 },
  retryText: { color: colors.ink, ...bangers(18) },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, marginHorizontal: 15, textAlign: "center" },
  blockedTitle: { color: colors.danger, ...bangers(19), lineHeight: 24, textAlign: "center" },
  blockedMessage: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 4, textAlign: "center" },
  keypad: { flexDirection: "row", flexWrap: "wrap", justifyContent: "space-between", marginTop: "auto", paddingBottom: 41, rowGap: 11, width: "100%" },
  key: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 14, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.ink}`, height: 61, justifyContent: "center", width: "30%" },
  keyPlaceholder: { height: 61, width: "30%" },
  keyText: { alignSelf: "stretch", color: colors.white, ...bangers(27), lineHeight: 34, paddingBottom: 1, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 },
  disabled: { opacity: 0.55 }, pressed: { opacity: 0.78, transform: [{ scale: 0.98 }] },
});
