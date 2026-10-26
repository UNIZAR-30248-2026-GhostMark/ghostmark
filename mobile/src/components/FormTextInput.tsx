import { Ionicons } from "@expo/vector-icons";
import type { ComponentProps } from "react";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { colors } from "../theme/colors";

type Props = ComponentProps<typeof TextInput> & { error?: string; label: string };

export function FormTextInput({ error, label, secureTextEntry, ...inputProps }: Props) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const isPassword = Boolean(secureTextEntry);
  return (
    <View style={styles.group}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.inputWrap}>
        <TextInput
          placeholderTextColor="#715F8D"
          secureTextEntry={isPassword && !passwordVisible}
          style={[styles.input, error && styles.inputError, isPassword && styles.passwordInput]}
          {...inputProps}
          {...(isPassword ? { autoCapitalize: "none", autoCorrect: false } : {})}
        />
        {isPassword ? <Pressable accessibilityLabel={passwordVisible ? "Ocultar contraseña" : "Mostrar contraseña"} accessibilityRole="button" onPress={() => setPasswordVisible((value) => !value)} style={styles.eye}>
          <Ionicons color={colors.ink} name={passwordVisible ? "eye-off-outline" : "eye-outline"} size={22} />
        </Pressable> : null}
      </View>
      {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  group: { gap: 6 },
  label: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13 },
  inputWrap: { position: "relative" },
  input: {
    backgroundColor: colors.white,
    borderColor: colors.ink,
    borderRadius: 13,
    borderWidth: 2,
    color: colors.ink,
    fontFamily: "ZenMaru",
    fontSize: 16,
    minHeight: 48,
    paddingHorizontal: 14,
  },
  passwordInput: { paddingRight: 50 },
  inputError: { borderColor: colors.danger },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 16 },
  eye: { alignItems: "center", height: 48, justifyContent: "center", position: "absolute", right: 4, top: 0, width: 44 },
});
