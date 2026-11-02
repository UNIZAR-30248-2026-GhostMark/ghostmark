import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { ApiError } from "../api/client";
import { createGame, requestAiMissionBatch } from "../api/game";
import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { MissionType } from "../types/game";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "CreateGameMissions">;
const contexts = ["Campus", "Fiesta de bienvenida", "Semana de exámenes", "Otro…"] as const;

export function CreateGameMissionsScreen({ navigation, route }: Props) {
  const [type, setType] = useState<MissionType>("IA");
  const [context, setContext] = useState<(typeof contexts)[number]>("Campus");
  const [otherContext, setOtherContext] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const contextValue = context === "Otro…" ? otherContext.trim() : context;

  async function createLibraryGame() {
    setError(undefined); setIsSaving(true);
    try {
      const game = await createGame({ ...route.params.configuration, tipoMisiones: "BIBLIOTECA" });
      openShare(game);
    } catch (requestError) {
      setError(messageFor(requestError, "No hemos podido crear la partida. Comprueba la conexión e inténtalo de nuevo."));
    } finally { setIsSaving(false); }
  }

  async function generateMissions() {
    if (contextValue.length < 3) { setError("Escribe un contexto de al menos 3 caracteres."); return; }
    setError(undefined); setIsSaving(true);
    try {
      const game = await createGame({ ...route.params.configuration, tipoMisiones: "IA" });
      const batch = await requestAiMissionBatch(game.id, contextValue);
      navigation.reset({ index: 1, routes: [homeRoute(game), { name: "ReviewAiMissions", params: { batch, contexto: contextValue, game } }] });
    } catch (requestError) {
      setError(messageFor(requestError, "No hemos podido generar las misiones. Comprueba la conexión e inténtalo de nuevo."));
    } finally { setIsSaving(false); }
  }

  function openShare(game: Parameters<typeof homeRoute>[0]) {
    navigation.reset({ index: 1, routes: [homeRoute(game), { name: "ShareGame", params: { game } }] });
  }

  return <Screen><View style={styles.screen}>
    <GameWizardHeader label="Misiones" onBack={() => navigation.goBack()} step={3} />
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <Text style={styles.heading}>MISIONES</Text>
      <View style={styles.typeSwitch}>
        <TypeButton active={type === "BIBLIOTECA"} label="BIBLIOTECA" onPress={() => setType("BIBLIOTECA")} />
        <TypeButton active={type === "IA"} label="GENERAR CON IA" onPress={() => setType("IA")} />
      </View>
      {type === "IA" ? <>
        <Text style={styles.contextHeading}>CONTEXTO</Text>
        <ScrollView contentContainerStyle={styles.contexts} horizontal showsHorizontalScrollIndicator={false}>{contexts.map((item) => <Pressable accessibilityRole="button" key={item} onPress={() => setContext(item)} style={({ pressed }) => [styles.contextChip, context === item && styles.contextChipOn, pressed && styles.pressed]}><Text style={[styles.contextText, context === item && styles.contextTextOn]}>{item}</Text></Pressable>)}</ScrollView>
        {context === "Otro…" ? <TextInput accessibilityLabel="Otro contexto para las misiones" maxLength={40} onChangeText={setOtherContext} placeholder="Describe el contexto" placeholderTextColor={colors.textMuted} style={styles.contextInput} value={otherContext} /> : null}
        <View style={styles.note}><Image source={require("../../assets/ghosts/fantasma-escudo.png")} style={styles.noteGhost} /><Text style={styles.noteText}>Siempre sin contacto físico. Tú apruebas cada misión antes de que entre en juego.</Text></View>
      </> : <View style={styles.libraryNote}><Text style={styles.libraryTitle}>MISIONES DE BIBLIOTECA</Text><Text style={styles.libraryText}>Usaremos las misiones revisadas de GhostMark, seguras y listas para repartir al arrancar.</Text></View>}
      {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
    </ScrollView>
    <View style={styles.bottomAction}><PrimaryButton disabled={isSaving} onPress={type === "IA" ? generateMissions : createLibraryGame}>{isSaving ? (type === "IA" ? "GENERANDO…" : "CREANDO…") : type === "IA" ? "GENERAR MISIONES" : "CREAR PARTIDA"}</PrimaryButton></View>
  </View></Screen>;
}

function TypeButton({ active, label, onPress }: { active: boolean; label: string; onPress: () => void }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.typeButton, active && styles.typeButtonOn, pressed && styles.pressed]}><Text style={[styles.typeText, active && styles.typeTextOn]}>{label}</Text></Pressable>;
}

function homeRoute(game: { id: string; nombre: string; inicioEn: string; finEn: string; estado: "EN_INSCRIPCION" | "EN_JUEGO" | "CANCELADA" | "CERRADA"; numInscritos: number; masterJuega: boolean }) {
  return { name: "Home" as const, params: { createdGame: { finEn: game.finEn, id: game.id, inicioEn: game.inicioEn, nombre: game.nombre, estado: game.estado, motivoCancelacion: null, numInscritos: game.numInscritos, soyJugador: game.masterJuega, soyMaster: true } } };
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? [error.message, ...error.fieldErrors].filter(Boolean).join("\n") : fallback;
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { flexGrow: 1, paddingTop: 26 }, bottomAction: { marginTop: 18 },
  heading: { color: colors.white, ...bangers(33) },
  typeSwitch: { backgroundColor: colors.ink, borderRadius: 16, flexDirection: "row", gap: 4, marginTop: 15, padding: 5 },
  typeButton: { alignItems: "center", borderRadius: 12, flex: 1, minHeight: 43, justifyContent: "center", paddingHorizontal: 8 },
  typeButtonOn: { backgroundColor: colors.lime, borderColor: colors.ink, borderWidth: 2, boxShadow: `0px 3px 0px ${colors.border}` },
  typeText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, textAlign: "center" },
  typeTextOn: { color: colors.ink, ...bangers(19) },
  contextHeading: { color: colors.white, ...bangers(28), marginTop: 28 },
  contexts: { alignItems: "center", gap: 10, marginHorizontal: -24, marginTop: 12, paddingHorizontal: 24, paddingVertical: 3 },
  contextChip: { alignSelf: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, minHeight: 43, paddingHorizontal: 14, paddingVertical: 8 },
  contextChipOn: { backgroundColor: colors.lime, borderColor: colors.ink, boxShadow: `0px 3px 0px ${colors.ink}` },
  contextText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13 }, contextTextOn: { color: colors.ink },
  contextInput: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 14, marginTop: 14, minHeight: 48, paddingHorizontal: 14 },
  note: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 17, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.border}`, flexDirection: "row", marginTop: 36, minHeight: 82, paddingHorizontal: 10, paddingVertical: 7 },
  noteGhost: { height: 92, marginLeft: -18, marginRight: 3, resizeMode: "contain", width: 82 },
  noteText: { color: colors.ink, flex: 1, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 19, paddingRight: 5 },
  libraryNote: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 18, borderWidth: 3, marginTop: 35, padding: 20 },
  libraryTitle: { color: colors.lime, ...bangers(27), textAlign: "center" }, libraryText: { color: colors.text, fontFamily: "ZenMaru", fontSize: 14, lineHeight: 21, marginTop: 12, textAlign: "center" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, marginTop: 20, textAlign: "center" },
  pressed: { opacity: 0.8, transform: [{ scale: 0.98 }] },
});
