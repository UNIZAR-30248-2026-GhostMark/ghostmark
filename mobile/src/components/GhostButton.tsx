import type { PropsWithChildren } from "react";
import { Pressable, StyleSheet, Text } from "react-native";

import { colors } from "../theme/colors";

type GhostButtonProps = PropsWithChildren<{
  onPress: () => void;
  variant?: "primary" | "secondary";
  disabled?: boolean;
}>;

export function GhostButton({ children, onPress, variant = "primary", disabled = false }: GhostButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        variant === "primary" ? styles.primary : styles.secondary,
        (pressed || disabled) && styles.pressed,
      ]}
    >
      <Text style={[styles.label, variant === "primary" ? styles.primaryLabel : styles.secondaryLabel]}>
        {children}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    borderRadius: 14,
    borderWidth: 2,
    minHeight: 54,
    justifyContent: "center",
    paddingHorizontal: 20,
  },
  primary: { backgroundColor: colors.lime, borderColor: colors.ink },
  secondary: { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
  pressed: { opacity: 0.75 },
  label: { fontFamily: "ZenMaruBold", fontSize: 15, textAlign: "center" },
  primaryLabel: { color: colors.ink },
  secondaryLabel: { color: colors.text },
});
