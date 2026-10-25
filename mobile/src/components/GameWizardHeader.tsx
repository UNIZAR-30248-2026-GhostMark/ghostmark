import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { colors } from "../theme/colors";

type Props = { step: 1 | 2 | 5; label: string; onBack: () => void };

export function GameWizardHeader({ step, label, onBack }: Props) {
  return <View><View style={styles.top}><Pressable accessibilityLabel="Volver" accessibilityRole="button" onPress={onBack} style={({ pressed }) => [styles.back, pressed && styles.pressed]}><Ionicons color={colors.text} name="arrow-back" size={23} /></Pressable><Text style={styles.title}>NUEVA PARTIDA</Text></View><View style={styles.progress}>{[1, 2, 3, 4, 5].map((item) => <View key={item} style={[styles.segment, item <= step && styles.segmentOn]} />)}</View><View style={styles.stepRow}><Text style={styles.stepText}><Text style={styles.stepStrong}>Paso {step}</Text> de 5</Text><Text style={styles.stepText}>{label}</Text></View></View>;
}

const styles = StyleSheet.create({
  top: { alignItems: "center", flexDirection: "row", gap: 14 }, back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, height: 44, justifyContent: "center", width: 44 }, title: { color: colors.text, fontFamily: "Bangers", fontSize: 27, letterSpacing: 0.8, paddingRight: 3 }, progress: { flexDirection: "row", gap: 8, marginTop: 17 }, segment: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 8, borderWidth: 2, flex: 1, height: 11 }, segmentOn: { backgroundColor: colors.lime, borderColor: colors.ink }, stepRow: { flexDirection: "row", justifyContent: "space-between", marginTop: 9 }, stepText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 }, stepStrong: { color: colors.text, fontFamily: "ZenMaruBold" }, pressed: { opacity: 0.78, transform: [{ scale: 0.96 }] },
});
