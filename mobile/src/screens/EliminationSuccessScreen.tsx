import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useMemo } from "react";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Svg, { G, Path, Rect } from "react-native-svg";

import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "EliminationSuccess">;

// Rayos y confeti de f-acierto, en el lienzo de 360 x 780 del prototipo.
const RAYS = "M180 330L-40 40H60zM180 330L140 -20h80zM180 330L400 40H300zM180 330L420 330V230zM180 330L-60 330V230zM180 330L400 620V520zM180 330L-40 620V520z";
const CONFETTI_COLORS = [colors.lime, colors.lime, colors.cream, colors.textMuted, colors.border, colors.ink];

export function confettiPieces(count = 46) {
  // Mismo generador fijo que el prototipo: el confeti sale siempre igual.
  let seed = 11;
  const random = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  return Array.from({ length: count }, (_, index) => ({
    color: CONFETTI_COLORS[index % CONFETTI_COLORS.length],
    x: random() * 350,
    y: 40 + random() * 330,
    rotation: random() * 180,
    opacity: 0.6 + random() * 0.4,
  }));
}

export function EliminationSuccessScreen({ navigation, route }: Props) {
  const { gameId, result } = route.params;
  const inherited = Boolean(result.carta.aliasObjetivo);
  const confetti = useMemo(() => confettiPieces(), []);

  return <View style={styles.screen}>
    <Svg pointerEvents="none" preserveAspectRatio="xMidYMin slice" style={StyleSheet.absoluteFill} viewBox="0 0 360 780">
      <Path d={RAYS} fill="rgba(20,7,31,0.08)" />
      <G>{confetti.map((piece, index) => <Rect fill={piece.color} height={18} key={index} opacity={piece.opacity} origin={`${piece.x + 5}, ${piece.y + 9}`} rotation={piece.rotation} rx={3} stroke={colors.ink} strokeWidth={2} width={10} x={piece.x} y={piece.y} />)}</G>
    </Svg>
    <SafeAreaView edges={["top", "right", "bottom", "left"]} style={styles.safeArea}>
      <View style={styles.content}>
        <Image accessibilityLabel="Fantasma eliminado" source={require("../../assets/ghosts/fantasma-ko.png")} style={styles.ghost} />
        <Text style={styles.title}>¡ELIMINADO!</Text>
        <Text style={styles.victim}>{result.aliasVictima ?? "Cuenta eliminada"} queda fuera de la partida</Text>
        <Text style={styles.inherit}>{inherited ? "Heredas su objetivo y su misión" : "Ya no tienes objetivo. ¡Eres la última persona en juego!"}</Text>
      </View>
      <View style={styles.footer}>
        <Pressable accessibilityRole="button" onPress={() => navigation.popTo("MissionCard", { gameId })} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
          <Text style={styles.buttonText}>{inherited ? "VER MI NUEVA CARTA" : "VER MI CARTA"}</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  </View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.coral, flex: 1, overflow: "hidden" },
  safeArea: { flex: 1 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24 },
  ghost: { height: 222, marginTop: 60, resizeMode: "contain", transform: [{ rotate: "-8deg" }], width: 236 },
  title: { alignSelf: "stretch", color: colors.cream, ...bangers(76), lineHeight: 84, marginTop: 100, textAlign: "center", textShadowColor: colors.ink, textShadowOffset: { height: 4, width: 0 }, textShadowRadius: 0 },
  victim: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 19, lineHeight: 25, marginTop: 10, textAlign: "center" },
  inherit: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 16, lineHeight: 22, marginTop: 6, textAlign: "center" },
  footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 },
  button: { alignItems: "center", backgroundColor: colors.cream, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, boxShadow: `4px 4px 0px ${colors.ink}`, height: 58, justifyContent: "center" },
  buttonText: { alignSelf: "stretch", color: colors.ink, ...bangers(24), letterSpacing: 0.8, textAlign: "center" },
  pressed: { opacity: 0.85, transform: [{ scale: 0.98 }] },
});
