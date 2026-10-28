import { Ionicons } from "@expo/vector-icons";
import { useIsFocused } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from "expo-camera";
import { useRef, useState } from "react";
import { ActivityIndicator, Linking, Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

import { Screen } from "../components/Screen";
import { parseInvitationLink } from "../utils/invitation";
import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "ScanInvitation">;

export function ScanInvitationScreen({ navigation }: Props) {
  const [permission, requestPermission] = useCameraPermissions();
  const isFocused = useIsFocused();
  const { width } = useWindowDimensions();
  const [message, setMessage] = useState<string>();
  const [cameraError, setCameraError] = useState(false);
  const scanLocked = useRef(false);
  const lastInvalidScan = useRef<{ data: string; at: number } | undefined>(undefined);
  const frameSize = Math.min(width - 48, 400);

  function handleBarcodeScanned({ data }: BarcodeScanningResult) {
    if (scanLocked.current) return;
    const code = parseInvitationLink(data);
    if (!code) {
      const now = Date.now();
      if (lastInvalidScan.current?.data === data && now - lastInvalidScan.current.at < 1000) return;
      lastInvalidScan.current = { data, at: now };
      setMessage("Este QR no contiene una invitación válida de GhostMark.");
      return;
    }
    scanLocked.current = true;
    navigation.replace("GameInvitation", { code });
  }

  const close = () => navigation.goBack();
  const title = <View style={styles.header}>
    <Pressable accessibilityLabel="Cerrar escáner" accessibilityRole="button" onPress={close} style={styles.closeButton}>
      <Ionicons color={colors.white} name="close" size={28} />
    </Pressable>
    <Text style={styles.heading}>UNIRSE A UNA PARTIDA</Text>
  </View>;

  if (!permission) {
    return <Screen padded={false}><View style={styles.container}>{title}<View style={styles.center}>
      <ActivityIndicator color={colors.lime} size="large" />
      <Text style={styles.message}>Comprobando el permiso de cámara…</Text>
    </View></View></Screen>;
  }

  if (!permission.granted || cameraError) {
    return <Screen padded={false}><View style={styles.container}>{title}<View style={styles.center}>
      <Text style={styles.message}>{cameraError
        ? "No se ha podido abrir la cámara. Comprueba que el dispositivo tiene una cámara disponible."
        : permission.canAskAgain
          ? "Permite usar la cámara para leer el QR de la invitación."
          : "La cámara está bloqueada. Activa el permiso en los ajustes del dispositivo."}</Text>
      {!cameraError && permission.canAskAgain ? <Pressable accessibilityRole="button" onPress={() => { void requestPermission(); }} style={styles.button}><Text style={styles.buttonText}>PERMITIR CÁMARA</Text></Pressable> : null}
      {!cameraError && !permission.canAskAgain ? <Pressable accessibilityRole="button" onPress={() => { void Linking.openSettings(); }} style={styles.button}><Text style={styles.buttonText}>ABRIR AJUSTES</Text></Pressable> : null}
      <Pressable accessibilityRole="button" onPress={close} style={styles.secondaryButton}><Text style={styles.secondaryText}>VOLVER</Text></Pressable>
    </View></View></Screen>;
  }

  return <Screen padded={false}><View style={styles.container}>
    {isFocused ? <CameraView
      barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
      facing="back"
      onBarcodeScanned={handleBarcodeScanned}
      onMountError={() => setCameraError(true)}
      style={StyleSheet.absoluteFill}
    /> : null}
    <View pointerEvents="none" style={styles.tint} />
    {title}
    <View style={styles.scanArea}>
      <View pointerEvents="none" style={[styles.frame, { height: frameSize, width: frameSize }]}>
        <View style={[styles.corner, styles.topLeft]} /><View style={[styles.corner, styles.topRight]} />
        <View style={[styles.corner, styles.bottomLeft]} /><View style={[styles.corner, styles.bottomRight]} />
      </View>
      <Text accessibilityLiveRegion="polite" style={[styles.instruction, message ? styles.invalid : null]}>{message ?? "Apunta al código QR de la partida"}</Text>
    </View>
    <Text style={styles.footer}>¿Tienes un enlace? Ábrelo y entrarás directamente.</Text>
  </View></Screen>;
}

const styles = StyleSheet.create({
  container: { backgroundColor: colors.background, flex: 1 },
  tint: { ...StyleSheet.absoluteFill, backgroundColor: "#1A0B2E66" },
  header: { alignItems: "center", flexDirection: "row", gap: 14, paddingHorizontal: 24, paddingTop: 22 },
  closeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 20, borderWidth: 3, height: 52, justifyContent: "center", width: 52 },
  heading: { color: colors.white, flex: 1, ...bangers(30), textShadowColor: colors.ink, textShadowOffset: { width: 2, height: 3 }, textShadowRadius: 0 },
  scanArea: { alignItems: "center", flex: 1, justifyContent: "center", paddingHorizontal: 24 },
  frame: { justifyContent: "center" },
  corner: { borderColor: colors.lime, height: 68, position: "absolute", width: 68 },
  topLeft: { borderLeftWidth: 9, borderTopWidth: 9, borderTopLeftRadius: 38, left: 0, top: 0 },
  topRight: { borderRightWidth: 9, borderTopWidth: 9, borderTopRightRadius: 38, right: 0, top: 0 },
  bottomLeft: { borderBottomWidth: 9, borderLeftWidth: 9, borderBottomLeftRadius: 38, bottom: 0, left: 0 },
  bottomRight: { borderBottomWidth: 9, borderRightWidth: 9, borderBottomRightRadius: 38, bottom: 0, right: 0 },
  instruction: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 12, borderWidth: 3, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 17, marginTop: 32, overflow: "hidden", paddingHorizontal: 18, paddingVertical: 12, textAlign: "center" },
  invalid: { color: colors.danger },
  footer: { color: colors.white, fontFamily: "ZenMaruBold", fontSize: 16, lineHeight: 23, paddingBottom: 28, paddingHorizontal: 35, textAlign: "center" },
  center: { alignItems: "center", flex: 1, gap: 22, justifyContent: "center", padding: 24 },
  message: { color: colors.text, fontFamily: "ZenMaru", fontSize: 17, lineHeight: 25, textAlign: "center" },
  button: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, justifyContent: "center", minHeight: 54, paddingHorizontal: 22, width: "100%" },
  buttonText: { alignSelf: "stretch", color: colors.ink, ...bangers(22), textAlign: "center" },
  secondaryButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, justifyContent: "center", minHeight: 54, width: "100%" },
  secondaryText: { alignSelf: "stretch", color: colors.text, ...bangers(22), textAlign: "center" },
});
