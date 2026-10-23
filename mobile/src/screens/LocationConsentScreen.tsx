import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as Location from "expo-location";
import { useState } from "react";
import { Image, StyleSheet, Text, View } from "react-native";

import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "LocationConsent">;

export function LocationConsentScreen({ navigation }: Props) {
  const [error, setError] = useState<string>();
  const [isRequesting, setIsRequesting] = useState(false);

  async function requestPermission() {
    setError(undefined);
    setIsRequesting(true);
    try {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (!permission.granted) {
        setError("Necesitamos el permiso de ubicación para comprobar las zonas seguras al eliminar.");
        return;
      }
      navigation.replace("AvailabilityConsent");
    } catch {
      setError("No hemos podido solicitar el permiso de ubicación. Inténtalo de nuevo.");
    } finally {
      setIsRequesting(false);
    }
  }

  return (
    <Screen padded={false}>
      <View style={styles.artwork}>
        <View style={styles.dots}><View style={styles.dot} /><View style={styles.dot} /><View style={[styles.dot, styles.dotOn]} /><View style={styles.dot} /></View>
        <View style={[styles.circle, styles.circle1]} /><View style={[styles.circle, styles.circle2]} /><View style={[styles.circle, styles.circle3]} />
        <Image accessibilityLabel="Fantasma protegido" source={require("../../assets/ghosts/fantasma-escudo.png")} style={styles.ghost} />
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>TU UBICACIÓN, A SALVO</Text>
        <Text style={styles.body}>Solo miro si estás en una zona segura en el momento de eliminar. Nunca guardo dónde estás.</Text>
        <Guarantee label="No se guarda tu posición" />
        <Guarantee label="Nadie ve dónde estás" />
        {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
      </View>
      <View style={styles.footer}><PrimaryButton disabled={isRequesting} onPress={requestPermission}>{isRequesting ? "SOLICITANDO…" : "DAR PERMISO"}</PrimaryButton></View>
    </Screen>
  );
}

// Garantías fijas, ya marcadas como en f1-a3: se muestran, no se preguntan.
function Guarantee({ label }: { label: string }) {
  return <View style={styles.checkRow}><View style={[styles.checkbox, styles.checkboxChecked]}><Text style={styles.checkmark}>✓</Text></View><Text style={styles.checkLabel}>{label}</Text></View>;
}

const styles = StyleSheet.create({
  artwork: { alignItems: "center", backgroundColor: colors.lime, height: 238, justifyContent: "flex-end", overflow: "hidden", position: "relative" },
  dots: { flexDirection: "row", gap: 7, left: 24, position: "absolute", top: 18, zIndex: 2 }, dot: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, height: 9, width: 21 }, dotOn: { backgroundColor: colors.surfaceRaised, width: 39 },
  circle: { backgroundColor: colors.limePale, borderRadius: 999, opacity: 0.55, position: "absolute" }, circle1: { height: 11, left: 64, top: 62, width: 11 }, circle2: { height: 14, right: 70, top: 92, width: 14 }, circle3: { bottom: 58, height: 10, left: 86, width: 10 },
  ghost: { height: 184, resizeMode: "contain", transform: [{ translateY: -20 }], width: 225 },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 20 }, title: { ...typography.title, fontSize: 34, lineHeight: 38 }, body: { ...typography.body, color: colors.textMuted, fontSize: 15, lineHeight: 22, marginTop: 10 },
  checkRow: { alignItems: "center", flexDirection: "row", gap: 12, marginTop: 19 }, checkbox: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 7, borderWidth: 2, height: 27, justifyContent: "center", width: 27 }, checkboxChecked: { backgroundColor: colors.lime }, checkmark: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 18 }, checkLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 15 },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 16, marginTop: 14, textAlign: "center" }, footer: { paddingBottom: 18, paddingHorizontal: 24, paddingTop: 10 },
});
