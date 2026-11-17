import { Modal, Pressable, StyleSheet, Text, View } from "react-native";

import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";

type Props = {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  destructive?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

/**
 * Confirmación con el estilo de GhostMark (R11-40): cuadro morado con borde, letra de la app y
 * los dos botones abajo. Sustituye a Alert.alert, que en web no enseña nada.
 */
export function ConfirmDialog({ visible, title, message, confirmLabel, cancelLabel = "Cancelar", destructive = false, onCancel, onConfirm }: Props) {
  return <Modal animationType="fade" onRequestClose={onCancel} transparent visible={visible}>
    <View style={styles.overlay}>
      <View accessibilityViewIsModal style={styles.dialog}>
        <Text accessibilityRole="header" style={styles.title}>{title.toUpperCase()}</Text>
        <Text style={styles.message}>{message}</Text>
        <View style={styles.actions}>
          <Pressable accessibilityLabel={cancelLabel} accessibilityRole="button" onPress={onCancel} style={({ pressed }) => [styles.cancel, pressed && styles.pressed]}>
            <Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.cancelText}>{cancelLabel.toUpperCase()}</Text>
          </Pressable>
          <Pressable accessibilityLabel={confirmLabel} accessibilityRole="button" onPress={onConfirm} style={({ pressed }) => [styles.confirm, destructive && styles.destructive, pressed && styles.pressed]}>
            <Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.confirmText}>{confirmLabel.toUpperCase()}</Text>
          </Pressable>
        </View>
      </View>
    </View>
  </Modal>;
}

const styles = StyleSheet.create({
  overlay: { alignItems: "center", backgroundColor: "rgba(8,2,15,.75)", flex: 1, justifyContent: "center", paddingHorizontal: 28 },
  dialog: { backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 19, borderWidth: 3, padding: 21, width: "100%" },
  title: { color: colors.text, ...bangers(25), textAlign: "center" },
  message: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 16, lineHeight: 23, marginTop: 14, textAlign: "center" },
  actions: { flexDirection: "row", gap: 10, marginTop: 17 },
  cancel: { alignItems: "center", borderColor: colors.textMuted, borderRadius: 10, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 48, paddingHorizontal: 7, paddingVertical: 4 },
  confirm: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 48, paddingHorizontal: 7, paddingVertical: 4 },
  destructive: { backgroundColor: colors.danger },
  cancelText: { alignSelf: "stretch", color: colors.text, ...bangers(15), lineHeight: 21, textAlign: "center" },
  confirmText: { alignSelf: "stretch", color: colors.ink, ...bangers(15), lineHeight: 21, textAlign: "center" },
  pressed: { opacity: 0.78, transform: [{ scale: 0.97 }] },
});
