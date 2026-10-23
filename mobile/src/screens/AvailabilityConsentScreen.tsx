import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Image, StyleSheet, Text, View } from "react-native";

import { saveAccountConsents } from "../api/account";
import { saveAccountConsents as storeAccountConsents } from "../api/auth";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "AvailabilityConsent">;

export function AvailabilityConsentScreen({ navigation }: Props) {
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  async function accept() {
    setError(undefined);
    setIsSaving(true);
    try {
      const account = await saveAccountConsents({ ubicacion: true, avisoDisponibilidad: true });
      storeAccountConsents(account);
      navigation.replace("Home");
    } catch {
      setError("No hemos podido guardar tus preferencias. Comprueba la conexión e inténtalo de nuevo.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Screen padded={false}>
      <View style={styles.artwork}>
        <View style={styles.dots}><View style={styles.dot} /><View style={styles.dot} /><View style={styles.dot} /><View style={[styles.dot, styles.dotOn]} /></View>
        <Text style={[styles.zzz, styles.zzz1]}>Z</Text><Text style={[styles.zzz, styles.zzz2]}>Z</Text><Text style={[styles.zzz, styles.zzz3]}>Z</Text>
        <Image accessibilityLabel="Fantasma descansando" source={require("../../assets/ghosts/fantasma-dormido.png")} style={styles.ghost} />
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>TU DISPONIBILIDAD</Text>
        <Text style={styles.body}>Tu cazador sabrá cuándo estás en clase, no dónde, para no perder intentos.</Text>
        <Text style={styles.detail}>Verá una cuenta atrás hasta que vuelvas a estar disponible. Nunca ve tu horario ni tu ubicación.</Text>
        {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
      </View>
      <View style={styles.footer}><PrimaryButton disabled={isSaving} onPress={accept}>{isSaving ? "GUARDANDO…" : "ACEPTO"}</PrimaryButton></View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  artwork: { alignItems: "center", backgroundColor: colors.availabilityArtwork, height: 238, justifyContent: "flex-end", overflow: "hidden", position: "relative" },
  dots: { flexDirection: "row", gap: 7, left: 24, position: "absolute", top: 18, zIndex: 2 }, dot: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, height: 9, width: 21 }, dotOn: { backgroundColor: colors.lime, width: 39 },
  zzz: { color: colors.text, fontFamily: "ZenMaruBold", opacity: 0.45, position: "absolute" }, zzz1: { fontSize: 18, right: 82, top: 74 }, zzz2: { fontSize: 25, right: 60, top: 54 }, zzz3: { fontSize: 14, right: 48, top: 96 }, ghost: { height: 190, resizeMode: "contain", transform: [{ translateY: -20 }], width: 235 },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 20 }, title: { ...typography.title, fontSize: 34, lineHeight: 38 }, body: { ...typography.body, color: colors.text, fontSize: 18, lineHeight: 28, marginTop: 16 }, detail: { ...typography.body, color: colors.textMuted, fontSize: 15, lineHeight: 23, marginTop: 20 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 16, marginTop: 14, textAlign: "center" }, footer: { paddingBottom: 18, paddingHorizontal: 24, paddingTop: 10 },
});
