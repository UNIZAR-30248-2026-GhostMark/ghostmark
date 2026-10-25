import { Ionicons } from "@expo/vector-icons";
import type { ComponentProps } from "react";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { colors } from "../theme/colors";

type Props = ComponentProps<typeof TextInput> & { label: string };

export function FormTextInput({ label, secureTextEntry, ...inputProps }: Props) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const isPassword = Boolean(secureTextEntry);
  return (
    <View style={styles.group}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.inputWrap}>
        <TextInput placeholderTextColor="#715F8D" secureTextEntry={isPassword && !passwordVisible} style={[styles.input, isPassword && styles.passwordInput]} {...inputProps} />
        {isPassword ? <Pressable accessibilityLabel={passwordVisible ? "Ocultar contraseña" : "Mostrar contraseña"} accessibilityRole="button" onPress={() => setPasswordVisible((value) => !value)} style={styles.eye}>
          <Ionicons color={colors.ink} name={passwordVisible ? "eye-off-outline" : "eye-outline"} size={22} />
        </Pressable> : null}
      </View>
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
  eye: { alignItems: "center", height: 48, justifyContent: "center", position: "absolute", right: 4, top: 0, width: 44 },
});
