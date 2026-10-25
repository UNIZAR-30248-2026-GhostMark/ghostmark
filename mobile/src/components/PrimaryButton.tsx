import type { PropsWithChildren } from "react";
import { Pressable, StyleSheet, Text } from "react-native";

import { colors } from "../theme/colors";

type Props = PropsWithChildren<{ disabled?: boolean; onPress: () => void }>;

export function PrimaryButton({ children, disabled = false, onPress }: Props) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [styles.button, (pressed || disabled) && styles.pressed]}
    >
      <Text style={styles.label}>{children}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    backgroundColor: colors.white,
    borderColor: colors.ink,
    borderRadius: 13,
    borderWidth: 2,
    minHeight: 52,
    justifyContent: "center",
    shadowColor: colors.border,
    shadowOffset: { width: 0, height: 5 },
    shadowOpacity: 1,
    shadowRadius: 0,
  },
  pressed: { opacity: 0.7 },
  label: { color: colors.ink, fontFamily: "Bangers", fontSize: 22, letterSpacing: 0.8, paddingRight: 3 },
});
