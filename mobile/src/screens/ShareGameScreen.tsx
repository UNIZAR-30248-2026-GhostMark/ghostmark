import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as Clipboard from "expo-clipboard";
import { useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from "react-native";
import QRCode from "react-native-qrcode-svg";

import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "ShareGame">;

export function ShareGameScreen({ navigation, route }: Props) {
  const { game } = route.params;
  // El enlace es el que devuelve el servidor (OpenAPI, `Partida.enlace`); la app no lo construye.
  const link = game.enlace;
  const { width } = useWindowDimensions();
  const [message, setMessage] = useState<string>();

  async function copyLink() {
    try {
      const copied = await Clipboard.setStringAsync(link);
      setMessage(copied ? "Enlace copiado." : "No se ha podido copiar. Mantén pulsado el enlace para seleccionarlo.");
    } catch {
      setMessage("No se ha podido copiar. Mantén pulsado el enlace para seleccionarlo.");
    }
  }


  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <GameWizardHeader label="Compartir" onBack={() => navigation.popToTop()} step={5} />
        <Text style={styles.heading}>¡PARTIDA CREADA!</Text>
        <View style={styles.card}>
          <View accessible accessibilityLabel={`QR de invitación a ${game.nombre}. Enlace: ${link}`} accessibilityRole="image">
            <QRCode value={link} size={Math.min(210, Math.max(120, width - 150))} quietZone={16} color="#000000" backgroundColor="#FFFFFF" />
          </View>
          {/* El código va junto al QR, visible sin desplazar (R11-35). Ocupa todo el ancho de la tarjeta y una sola línea: en Android el ancho se mide sin el espaciado entre letras y el último carácter se cortaba (#257). */}
          <Text accessibilityLabel={`Código de la partida: ${game.codigoInvitacion}`} numberOfLines={1} selectable style={styles.cardCode}>{game.codigoInvitacion}</Text>
          <View style={styles.cardFooter}>
            <Text style={styles.gameName}>{game.nombre}</Text>
            <Image accessible={false} source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.ghost} />
          </View>
        </View>
        <Text selectable style={styles.link}>{link}</Text>
        {message ? <Text accessibilityLiveRegion="polite" style={styles.message}>{message}</Text> : null}
      </ScrollView>
      <View style={styles.actions}>
        <Pressable accessibilityRole="button" onPress={copyLink} style={({ pressed }) => [styles.copyButton, pressed && styles.pressed]}>
          <Ionicons name="link-outline" size={25} color={colors.text} />
          <Text style={styles.copyLabel}>COPIAR ENLACE</Text>
        </Pressable>
        <PrimaryButton onPress={() => navigation.replace("WaitingRoom", { gameId: game.id, game })}>IR A LA PARTIDA</PrimaryButton>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, paddingBottom: 12 },
  heading: { color: colors.lime, ...bangers(39), marginVertical: 25, textAlign: "center" },
  card: { alignItems: "center", alignSelf: "center", backgroundColor: "#FFFFFF", borderColor: colors.ink, borderRadius: 25, borderWidth: 3, boxShadow: `0px 6px 0px ${colors.ink}`, padding: 10 },
  cardFooter: { alignItems: "center", flexDirection: "row", width: "100%" },
  gameName: { flex: 1, color: colors.ink, ...bangers(25), maxWidth: "100%", paddingHorizontal: 12, paddingBottom: 12, textAlign: "center" },
  ghost: { height: 60, resizeMode: "contain", width: 60 },
  cardCode: { alignSelf: "stretch", color: colors.ink, ...bangers(30), letterSpacing: 2, marginTop: 4, textAlign: "center" },
  link: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, marginTop: 8, textAlign: "center" },
  message: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13, textAlign: "center" },
  actions: { gap: 14, paddingTop: 12, paddingBottom: 4 },
  copyButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, flexDirection: "row", gap: 12, justifyContent: "center", minHeight: 54 },
  copyLabel: { color: colors.text, flex: 1, ...bangers(23), textAlign: "center" },
  pressed: { opacity: 0.75 },
});
