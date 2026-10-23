import type { ComponentProps } from "react";
import { StyleSheet, Text, TextInput, View } from "react-native";

import { colors } from "../theme/colors";

type Props = ComponentProps<typeof TextInput> & { label: string };

export function FormTextInput({ label, ...inputProps }: Props) {
  return (
    <View style={styles.group}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        placeholderTextColor="#715F8D"
        style={styles.input}
        {...inputProps}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  group: { gap: 6 },
  label: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13 },
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
});
