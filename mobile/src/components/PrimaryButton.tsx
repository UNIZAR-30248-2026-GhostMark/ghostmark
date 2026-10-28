import type { PropsWithChildren } from "react";
import { Pressable, StyleSheet, Text } from "react-native";

import { colors } from "../theme/colors";
import { bangers } from "../theme/bangers";

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
    boxShadow: `0px 5px 0px ${colors.border}`,
  },
  pressed: { opacity: 0.7 },
  label: { color: colors.ink, ...bangers(22), letterSpacing: 0.8 },
});
