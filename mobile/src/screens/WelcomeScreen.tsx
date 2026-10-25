import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useRef, useState } from "react";
import { Image, Keyboard, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { registerAccount, saveSession } from "../api/auth";
import { FormTextInput } from "../components/FormTextInput";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Welcome">;

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    return [error.message, ...error.fieldErrors].join("\n");
  }
  return "No hemos podido crear la cuenta. Revisa la conexión e inténtalo de nuevo.";
}

export function WelcomeScreen({ navigation }: Props) {
  const [alias, setAlias] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [keyboardVisible, setKeyboardVisible] = useState(false);
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    const show = Keyboard.addListener(Platform.OS === "ios" ? "keyboardWillShow" : "keyboardDidShow", () => setKeyboardVisible(true));
    const hide = Keyboard.addListener(Platform.OS === "ios" ? "keyboardWillHide" : "keyboardDidHide", () => setKeyboardVisible(false));
    return () => { show.remove(); hide.remove(); };
  }, []);

  function revealFocusedField() {
    requestAnimationFrame(() => scrollRef.current?.scrollToEnd({ animated: true }));
  }

  async function register() {
    if (!alias.trim() || !email.trim() || !password) {
      setError("Completa alias, email y contraseña para continuar.");
      return;
    }

    setError(undefined);
    setIsSubmitting(true);
    try {
      const session = await registerAccount({ alias: alias.trim(), email: email.trim(), contrasena: password });
      await saveSession(session);
      navigation.replace("Schedule", { onboarding: true });
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Screen padded={false}>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"} style={styles.flex}>
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardDismissMode="interactive"
          keyboardShouldPersistTaps="handled"
          ref={scrollRef}
          scrollEnabled={keyboardVisible}
          showsVerticalScrollIndicator={false}
        >
        <View style={[styles.artwork, styles.artworkCompact]}>
          <View style={styles.dots}><View style={[styles.dot, styles.dotOn]} /><View style={styles.dot} /><View style={styles.dot} /><View style={styles.dot} /></View>
          <Image source={require("../../assets/ghosts/fantasma-saluda.png")} accessibilityLabel="Fantasma saludando" style={styles.ghostCompact} />
        </View>

        <View style={styles.bodyContent}>
          <Text style={styles.title}>TE DAMOS LA{"\n"}BIENVENIDA A{"\n"}<Text style={styles.accent}>GHOSTMARK</Text></Text>
          <Text style={styles.body}>Elige cómo te verán los demás</Text>
          <View style={styles.form}>
            <FormTextInput label="Tu alias" value={alias} onChangeText={setAlias} onFocus={revealFocusedField} placeholder="fantasmilla" autoCapitalize="none" autoCorrect={false} />
            <FormTextInput label="Email" value={email} onChangeText={setEmail} onFocus={revealFocusedField} placeholder="tu@email.com" autoCapitalize="none" autoComplete="email" keyboardType="email-address" />
            <FormTextInput label="Contraseña" value={password} onChangeText={setPassword} onFocus={revealFocusedField} placeholder="Mínimo 8 caracteres" autoComplete="new-password" secureTextEntry />
            {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
          </View>
        </View>
        </ScrollView>
        <View style={styles.footer}>
          <PrimaryButton disabled={isSubmitting} onPress={register}>{isSubmitting ? "CREANDO CUENTA…" : "CONTINUAR"}</PrimaryButton>
          <Pressable accessibilityRole="link" onPress={() => navigation.navigate("SignIn")}><Text style={styles.loginLink}>¿Ya tienes cuenta? Inicia sesión</Text></Pressable>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 }, content: { flexGrow: 1 },
  artwork: { alignItems: "center", backgroundColor: colors.textMuted, height: 238, justifyContent: "flex-end", overflow: "hidden", position: "relative" }, artworkCompact: { height: 124 },
  dots: { flexDirection: "row", gap: 7, left: 24, position: "absolute", top: 18, zIndex: 1 }, dot: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, height: 9, width: 21 }, dotOn: { backgroundColor: colors.lime, width: 39 },
  ghostCompact: { height: 94, resizeMode: "contain", transform: [{ translateY: -6 }], width: 87 },
  bodyContent: { gap: 8, paddingHorizontal: 24, paddingTop: 14 }, title: { ...typography.title, fontSize: 32, lineHeight: 35 }, accent: { color: colors.lime }, body: { ...typography.body, color: colors.textMuted, fontSize: 15 }, form: { gap: 9, marginTop: 6 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 17 },
  footer: { gap: 10, paddingBottom: 14, paddingHorizontal: 24, paddingTop: 10 }, loginLink: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 14, textAlign: "center" },
});
