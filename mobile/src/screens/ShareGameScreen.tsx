import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as Clipboard from "expo-clipboard";
import { useState } from "react";
import { Image, Pressable, ScrollView, Share, StyleSheet, Text, useWindowDimensions, View } from "react-native";
import QRCode from "react-native-qrcode-svg";

import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";
import { invitationLink } from "../utils/invitation";

type Props = NativeStackScreenProps<RootStackParamList, "ShareGame">;

export function ShareGameScreen({ navigation, route }: Props) {
  const { game } = route.params;
  const link = invitationLink(game.codigoInvitacion);
  const { width } = useWindowDimensions();
  const [message, setMessage] = useState<string>();
  const [sharing, setSharing] = useState(false);

  async function copyLink() {
    try {
      const copied = await Clipboard.setStringAsync(link);
      setMessage(copied ? "Enlace copiado." : "No se ha podido copiar. Mantén pulsado el enlace para seleccionarlo.");
    } catch {
      setMessage("No se ha podido copiar. Mantén pulsado el enlace para seleccionarlo.");
    }
  }

  async function shareLink() {
    setSharing(true);
    setMessage(undefined);
    try {
      await Share.share({ title: game.nombre, message: `Únete a «${game.nombre}» en GhostMark: ${link}` });
    } catch {
      setMessage("No se ha podido compartir. Puedes copiar el enlace.");
    } finally {
      setSharing(false);
    }
  }

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <GameWizardHeader label="Compartir" onBack={() => navigation.popToTop()} step={5} />
        <Text style={styles.heading}>¡PARTIDA CREADA!</Text>
        <View style={styles.card}>
          <View accessible accessibilityLabel={`QR de invitación a ${game.nombre}. Enlace: ${link}`} accessibilityRole="image">
            <QRCode value={link} size={Math.min(240, Math.max(120, width - 130))} quietZone={16} color="#000000" backgroundColor="#FFFFFF" />
          </View>
          <View style={styles.cardFooter}>
            <Text style={styles.gameName}>{game.nombre}</Text>
            <Image accessible={false} source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.ghost} />
          </View>
        </View>
        <Text style={styles.code}>Código: {game.codigoInvitacion}</Text>
        <Text selectable style={styles.link}>{link}</Text>
        <Pressable accessibilityRole="button" disabled={sharing} onPress={shareLink} style={styles.share}>
          <Ionicons name="share-social-outline" color={colors.lime} size={20} />
          <Text style={styles.shareText}>COMPARTIR INVITACIÓN</Text>
        </Pressable>
        {message ? <Text accessibilityLiveRegion="polite" style={styles.message}>{message}</Text> : null}
      </ScrollView>
      <View style={styles.actions}>
        <Pressable accessibilityRole="button" onPress={copyLink} style={({ pressed }) => [styles.copyButton, pressed && styles.pressed]}>
          <Ionicons name="link-outline" size={25} color={colors.text} />
          <Text style={styles.copyLabel}>COPIAR ENLACE</Text>
        </Pressable>
        <PrimaryButton onPress={() => navigation.replace("GameInvitation", { code: game.codigoInvitacion })}>IR A LA PARTIDA</PrimaryButton>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, paddingBottom: 12 },
  heading: { color: colors.lime, fontFamily: "Bangers", fontSize: 39, marginVertical: 25, textAlign: "center" },
  card: { alignItems: "center", alignSelf: "center", backgroundColor: "#FFFFFF", borderColor: colors.ink, borderRadius: 25, borderWidth: 3, padding: 10, shadowColor: colors.ink, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.7, shadowRadius: 0 },
  cardFooter: { alignItems: "center", flexDirection: "row", width: "100%" },
  gameName: { flex: 1, color: colors.ink, fontFamily: "Bangers", fontSize: 25, maxWidth: "100%", paddingHorizontal: 12, paddingBottom: 12, textAlign: "center" },
  ghost: { height: 60, resizeMode: "contain", width: 60 },
  code: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16, marginTop: 20, textAlign: "center" },
  link: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, marginTop: 8, textAlign: "center" },
  share: { alignItems: "center", alignSelf: "center", flexDirection: "row", gap: 8, minHeight: 48, marginTop: 8 },
  shareText: { color: colors.lime, fontFamily: "Bangers", fontSize: 19 },
  message: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13, textAlign: "center" },
  actions: { gap: 14, paddingTop: 12, paddingBottom: 4 },
  copyButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, flexDirection: "row", gap: 12, justifyContent: "center", minHeight: 54 },
  copyLabel: { color: colors.text, fontFamily: "Bangers", fontSize: 23 },
  pressed: { opacity: 0.75 },
});
