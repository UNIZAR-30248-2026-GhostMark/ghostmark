import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useRef, useState } from "react";
import { Image, Keyboard, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { login, nextAccountScreen, saveSession } from "../api/auth";
import { ApiError } from "../api/client";
import { FormTextInput } from "../components/FormTextInput";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "SignIn">;

export function SignInScreen({ navigation }: Props) {
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

  async function signIn() {
    if (!email.trim() || !password) { setError("Introduce tu email y contraseña."); return; }
    setError(undefined); setIsSubmitting(true);
    try {
      const session = await login({ email: email.trim(), contrasena: password });
      await saveSession(session);
      navigation.replace(nextAccountScreen());
    } catch (requestError) {
      if (requestError instanceof ApiError) setError([requestError.message, ...requestError.fieldErrors].join("\n"));
      else setError("No hemos podido iniciar sesión. Revisa la conexión e inténtalo de nuevo.");
    } finally { setIsSubmitting(false); }
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
        <View style={[styles.artwork, keyboardVisible && styles.artworkCompact]}>
          <View style={[styles.bgCircle, styles.bgCircle1]} />
          <View style={[styles.bgCircle, styles.bgCircle2]} />
          <View style={[styles.bgCircle, styles.bgCircle3]} />
          <View style={[styles.bgCircle, styles.bgCircle4]} />
          <View style={[styles.bgCircle, styles.bgCircle5]} />
          <View style={[styles.bgCircle, styles.bgCircle6]} />
          <Image source={require("../../assets/ghosts/fantasma-picaro.png")} accessibilityLabel="Fantasma pícaro" style={styles.ghost} />
          <View style={styles.helloBubble}><Text style={styles.helloText}>¡hola!</Text></View>
        </View>
        <View style={styles.bodyContent}>
          <Text style={styles.title}>QUÉ BIEN VERTE{"\n"}DE NUEVO</Text>
          <Text style={styles.body}>Inicia sesión para volver a tus partidas.</Text>
          <View style={styles.form}>
            <FormTextInput label="Email" value={email} onChangeText={setEmail} onFocus={revealFocusedField} placeholder="tu@email.com" autoCapitalize="none" autoComplete="email" keyboardType="email-address" />
            <FormTextInput label="Contraseña" value={password} onChangeText={setPassword} onFocus={revealFocusedField} placeholder="Tu contraseña" autoComplete="current-password" secureTextEntry />
            {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
          </View>
        </View>
        </ScrollView>
        <View style={styles.footer}>
          <PrimaryButton disabled={isSubmitting} onPress={signIn}>{isSubmitting ? "ENTRANDO…" : "INICIAR SESIÓN"}</PrimaryButton>
          <Pressable accessibilityRole="link" onPress={() => navigation.navigate("Welcome")}><Text style={styles.registerLink}>¿No tienes cuenta? Crear cuenta</Text></Pressable>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 }, content: { flexGrow: 1 }, artwork: { alignItems: "center", backgroundColor: colors.signInArtwork, height: 238, justifyContent: "flex-end", overflow: "hidden", position: "relative" }, artworkCompact: { height: 106 },
  bgCircle: { backgroundColor: colors.signInDecoration, borderRadius: 999, opacity: 0.7, position: "absolute" },
  bgCircle1: { height: 10, left: 62, top: 58, width: 10 }, bgCircle2: { height: 8, left: 188, top: 46, width: 8 }, bgCircle3: { height: 12, right: 78, top: 86, width: 12 }, bgCircle4: { bottom: 70, height: 11, left: 80, width: 11 }, bgCircle5: { bottom: 52, height: 14, right: 70, width: 14 }, bgCircle6: { height: 6, right: 126, top: 146, width: 6 },
  ghost: { height: 190, resizeMode: "contain", transform: [{ translateX: -6 }, { translateY: -12 }], width: 235 }, helloBubble: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 12, borderWidth: 2, paddingHorizontal: 14, paddingVertical: 5, position: "absolute", right: 32, top: 60, transform: [{ rotate: "8deg" }] }, helloText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 14 },
  bodyContent: { gap: 10, paddingHorizontal: 24, paddingTop: 22 }, title: { ...typography.title, fontSize: 34, lineHeight: 37 }, body: { ...typography.body, color: colors.textMuted, fontSize: 15 }, form: { gap: 11, marginTop: 8 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 17 },
  footer: { gap: 12, paddingBottom: 14, paddingHorizontal: 24, paddingTop: 12 }, registerLink: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 14, textAlign: "center" },
});
