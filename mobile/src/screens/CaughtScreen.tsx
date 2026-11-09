import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useMemo } from "react";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Svg, { G, Line } from "react-native-svg";

import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";
import { markCaughtSeen } from "../utils/caughtSeen";

type Props = NativeStackScreenProps<RootStackParamList, "Caught">;

// Contorno de tinta del rótulo, como en g-cazado: copias desplazadas detrás del texto.
const OUTLINE = [[-5, 0], [5, 0], [0, -5], [0, 5], [-4, -4], [4, -4], [-4, 4], [4, 4], [0, 8]] as const;

export function actionLines(count = 64) {
  // Líneas de acción de g-cazado con el mismo generador fijo que el prototipo.
  let seed = 7;
  const random = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  return Array.from({ length: count }, (_, index) => {
    const angle = (index / count) * Math.PI * 2 + random() * 0.05;
    const start = 70 + random() * 40;
    return { x1: Math.cos(angle) * start, y1: Math.sin(angle) * start, x2: Math.cos(angle) * 260, y2: Math.sin(angle) * 260, width: 1 + random() * 3 };
  });
}

export function CaughtScreen({ navigation, route }: Props) {
  const { gameId } = route.params;
  const lines = useMemo(() => actionLines(), []);

  // Se enseña una vez por partida (R11-25): desde ahora la carta es la del espectador.
  useEffect(() => { void markCaughtSeen(gameId); }, [gameId]);

  return <View style={styles.screen}>
    <Svg pointerEvents="none" style={styles.lines} viewBox="-180 -180 360 360">
      <G stroke="#FFFFFF" strokeLinecap="round">{lines.map((line, index) => <Line key={index} strokeWidth={line.width} x1={line.x1} x2={line.x2} y1={line.y1} y2={line.y2} />)}</G>
    </Svg>
    <SafeAreaView edges={["top", "right", "bottom", "left"]} style={styles.safeArea}>
      <View style={styles.content}>
        <Image accessibilityLabel="Fantasma cazado" source={require("../../assets/ghosts/fantasma-ko.png")} style={styles.ghost} />
        <View accessibilityRole="header" style={styles.titleWrap}>
          {OUTLINE.map(([x, y]) => <Text accessibilityElementsHidden aria-hidden importantForAccessibility="no-hide-descendants" key={`${x},${y}`} style={[styles.title, styles.titleOutline, { transform: [{ translateX: x }, { translateY: y }] }]}>TE HAN{"\n"}CAZADO</Text>)}
          <Text style={styles.title}>TE HAN{"\n"}CAZADO</Text>
        </View>
      </View>
      <View style={styles.footer}>
        {/* Sin disputa (R11-14), «CERRAR» es el botón principal (R11-29). */}
        <Pressable accessibilityRole="button" onPress={() => { void markCaughtSeen(gameId).then(() => navigation.goBack()); }} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
          <Text style={styles.buttonText}>CERRAR</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  </View>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.coral, flex: 1, overflow: "hidden" },
  lines: { alignSelf: "center", height: 560, opacity: 0.28, position: "absolute", top: -40, width: 560 },
  safeArea: { flex: 1 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24 },
  ghost: { height: 222, marginTop: 40, resizeMode: "contain", transform: [{ rotate: "6deg" }], width: 236 },
  titleWrap: { alignSelf: "stretch", marginTop: 70 },
  title: { alignSelf: "stretch", color: colors.cream, ...bangers(64), lineHeight: 66, textAlign: "center" },
  titleOutline: { color: colors.ink, left: 0, position: "absolute", right: 0, top: 0 },
  footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 },
  button: { alignItems: "center", backgroundColor: colors.cream, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, boxShadow: `4px 4px 0px ${colors.ink}`, height: 58, justifyContent: "center" },
  buttonText: { alignSelf: "stretch", color: colors.ink, ...bangers(24), letterSpacing: 0.8, textAlign: "center" },
  pressed: { opacity: 0.85, transform: [{ scale: 0.98 }] },
});
