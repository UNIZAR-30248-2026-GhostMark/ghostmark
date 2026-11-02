import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import Svg, { Circle } from "react-native-svg";

import { ApiError } from "../api/client";
import { getMyLifeCode, type LifeCode } from "../api/game";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";
import { lifeCodeSecondsRemaining } from "../utils/lifeCode";

type Props = NativeStackScreenProps<RootStackParamList, "LifeCode">;
const RING_RADIUS = 122;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

export function LifeCodeScreen({ navigation, route }: Props) {
  const [lifeCode, setLifeCode] = useState<LifeCode>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);
  const [remaining, setRemaining] = useState(0);

  useEffect(() => {
    let active = true;
    getMyLifeCode(route.params.gameId).then((result) => {
      if (!active) return;
      setLifeCode(result);
      setRemaining(lifeCodeSecondsRemaining(result.caducaEn));
    }).catch((requestError: unknown) => {
      if (!active) return;
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar tu código. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [attempt, route.params.gameId]);

  useEffect(() => {
    if (!lifeCode) return undefined;
    const timer = setInterval(() => {
      const next = lifeCodeSecondsRemaining(lifeCode.caducaEn);
      setRemaining(next);
      if (next === 0) setAttempt((value) => value + 1);
    }, 1_000);
    return () => clearInterval(timer);
  }, [lifeCode]);

  function retry() {
    setError(undefined);
    setAttempt((value) => value + 1);
  }

  const progress = Math.min(1, remaining / 60);
  return <Screen padded={false}><View style={styles.screen}>
    <Checkerboard />
    <Pressable accessibilityLabel="Cerrar mi código" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.closeButton}><Text style={styles.closeText}>×</Text></Pressable>
    <View style={styles.content}>
      <Image accessibilityLabel="Fantasma con café protegiendo el código" source={require("../../assets/ghosts/fantasma-asustado.png")} style={styles.ghost} />
      <View style={styles.ringWrap}>
        <Svg height={286} width={286}>
          <Circle cx={143} cy={143} fill={colors.ink} r={RING_RADIUS} stroke="#3C3153" strokeWidth={20} />
          <Circle cx={143} cy={143} fill="none" r={RING_RADIUS} rotation="-90" stroke={colors.lime} strokeDasharray={RING_CIRCUMFERENCE} strokeDashoffset={RING_CIRCUMFERENCE * (1 - progress)} strokeLinecap="round" strokeWidth={20} origin="143, 143" />
        </Svg>
        <View style={styles.ringText}><Text style={styles.codeLabel}>Tu código</Text><Text accessibilityLabel={lifeCode ? `Código de vida ${lifeCode.codigo}` : "Código de vida cargando"} style={styles.code}>{lifeCode ? <>{lifeCode.codigo.slice(0, 3)} {lifeCode.codigo.slice(3)}</> : "··· ···"}</Text><Text accessibilityLiveRegion="polite" style={styles.seconds}>{lifeCode ? `${remaining} s` : "Cargando…"}</Text></View>
      </View>
      {error ? <View style={styles.errorState}><Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text><Pressable accessibilityRole="button" onPress={retry} style={({ pressed }) => [styles.retryButton, pressed && styles.retryPressed]}><Text style={styles.retryText}>REINTENTAR</Text></Pressable></View> : null}
      <Text style={styles.turnPhone}>GIRA EL MÓVIL HACIA QUIEN{`\n`}TE HA CAZADO</Text>
    </View>
  </View></Screen>;
}

function Checkerboard() {
  return <View pointerEvents="none" style={styles.checkerboard}>{Array.from({ length: 240 }, (_, index) => <View key={index} style={[styles.square, index % 2 === Math.floor(index / 12) % 2 && styles.squareLight]} />)}</View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: "#7E37BE", flex: 1, overflow: "hidden" },
  checkerboard: { flexDirection: "row", flexWrap: "wrap", height: "100%", left: 0, opacity: 0.16, position: "absolute", top: 0, width: "100%" },
  square: { aspectRatio: 1, backgroundColor: "#4A2074", width: "8.333%" }, squareLight: { backgroundColor: "#D6A6FF" },
  closeButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 11, borderWidth: 2, boxShadow: `0px 3px 0px ${colors.border}`, height: 42, justifyContent: "center", left: 22, position: "absolute", top: 34, width: 42, zIndex: 2 },
  closeText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 28, lineHeight: 30, marginTop: -3 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 26 },
  ghost: { height: 154, marginTop: 42, resizeMode: "contain", width: 166 },
  ringWrap: { alignItems: "center", height: 286, justifyContent: "center", marginTop: 7, width: 286 },
  ringText: { alignItems: "center", justifyContent: "center", position: "absolute" },
  codeLabel: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 14 },
  code: { color: colors.white, ...bangers(48), letterSpacing: 1, marginTop: 8 },
  seconds: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 14, marginTop: 8 },
  turnPhone: { alignSelf: "stretch", color: colors.white, ...bangers(29), lineHeight: 32, marginTop: "auto", paddingBottom: 42, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 2, width: 1 }, textShadowRadius: 0 },
  errorState: { alignItems: "center", marginTop: 7, width: "100%" },
  error: { color: colors.white, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 17, marginBottom: 8, textAlign: "center" },
  retryButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, boxShadow: `0px 3px 0px ${colors.border}`, justifyContent: "center", minHeight: 38, paddingHorizontal: 26 },
  retryPressed: { opacity: 0.8, transform: [{ scale: 0.98 }] },
  retryText: { color: colors.ink, ...bangers(18) },
});
