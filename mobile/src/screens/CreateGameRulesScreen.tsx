import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { createGame } from "../api/game";
import { ApiError } from "../api/client";
import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "CreateGameRules">;
const deadlines: readonly (2 | 6 | 12 | 24)[] = [2, 6, 12, 24];

export function CreateGameRulesScreen({ navigation, route }: Props) {
  const [masterJuega, setMasterJuega] = useState(true);
  const [entradasTardias, setEntradasTardias] = useState(false);
  const [plazoDisputaHoras, setPlazoDisputaHoras] = useState<2 | 6 | 12 | 24>(12);
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  async function saveGame() {
    setError(undefined); setIsSaving(true);
    try {
      const game = await createGame({ ...route.params.game, entradasTardias, masterJuega, plazoDisputaHoras, tipoMisiones: "BIBLIOTECA" });
      navigation.reset({
        index: 1,
        routes: [
          {
            name: "Home",
            params: {
              createdGame: {
                finEn: game.finEn,
                id: game.id,
                inicioEn: game.inicioEn,
                nombre: game.nombre,
                estado: game.estado,
                numInscritos: game.numInscritos,
                soyMaster: true,
                soyJugador: masterJuega,
                motivoCancelacion: null,
              },
            },
          },
          { name: "ShareGame", params: { game } },
        ],
      });
    } catch (requestError) {
      setError(requestError instanceof ApiError ? [requestError.message, ...requestError.fieldErrors].join("\n") : "No hemos podido crear la partida. Comprueba la conexión e inténtalo de nuevo.");
    } finally { setIsSaving(false); }
  }

  return <Screen><View style={styles.screen}><GameWizardHeader label="Reglas" onBack={() => navigation.goBack()} step={2} /><View style={styles.content}><Text style={styles.heading}>REGLAS</Text><RuleRow detail="No verás nada que no te toque como jugador" label="Yo también juego" onPress={() => setMasterJuega((value) => !value)} value={masterJuega} /><RuleRow label="Permitir entradas tardías" onPress={() => setEntradasTardias((value) => !value)} value={entradasTardias} /><Text style={styles.deadlineLabel}>PLAZO PARA RESOLVER DISPUTAS</Text><View style={styles.chips}>{deadlines.map((hours) => <Pressable accessibilityRole="button" key={hours} onPress={() => setPlazoDisputaHoras(hours)} style={({ pressed }) => [styles.chip, plazoDisputaHoras === hours && styles.chipOn, pressed && styles.pressed]}><Text style={[styles.chipText, plazoDisputaHoras === hours && styles.chipTextOn]}>{hours} h</Text></Pressable>)}</View>{error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}</View><PrimaryButton disabled={isSaving} onPress={saveGame}>{isSaving ? "CREANDO…" : "CREAR PARTIDA"}</PrimaryButton></View></Screen>;
}

function RuleRow({ label, detail, value, onPress }: { label: string; detail?: string; value: boolean; onPress: () => void }) { return <Pressable accessibilityRole="switch" accessibilityState={{ checked: value }} onPress={onPress} style={({ pressed }) => [styles.rule, pressed && styles.pressed]}><View style={styles.ruleText}><Text style={styles.ruleLabel}>{label}</Text>{detail ? <Text style={styles.ruleDetail}>{detail}</Text> : null}</View><View style={[styles.toggle, value && styles.toggleOn]}><View style={[styles.thumb, value && styles.thumbOn]} /></View></Pressable>; }

const styles = StyleSheet.create({
  screen: { flex: 1 }, content: { flex: 1, paddingTop: 27 }, heading: { ...typography.title, fontSize: 34 }, rule: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, flexDirection: "row", gap: 12, marginTop: 18, minHeight: 76, paddingHorizontal: 15, paddingVertical: 12, shadowColor: colors.ink, shadowOffset: { height: 4, width: 0 }, shadowOpacity: 0.45, shadowRadius: 0 }, ruleText: { flex: 1 }, ruleLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16 }, ruleDetail: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 18, marginTop: 4 }, toggle: { backgroundColor: colors.background, borderColor: colors.ink, borderRadius: 13, borderWidth: 3, height: 30, justifyContent: "center", paddingHorizontal: 3, width: 52 }, toggleOn: { backgroundColor: colors.lime }, thumb: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, height: 21, width: 21 }, thumbOn: { alignSelf: "flex-end" }, deadlineLabel: { color: colors.text, fontFamily: "Bangers", fontSize: 25, letterSpacing: 0.6, marginTop: 29, paddingRight: 3 }, chips: { flexDirection: "row", gap: 8, marginTop: 13 }, chip: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, flex: 1, paddingVertical: 9 }, chipOn: { backgroundColor: colors.lime, borderColor: colors.ink }, chipText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 13 }, chipTextOn: { color: colors.ink }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 17, marginTop: 16, textAlign: "center" }, pressed: { opacity: 0.82, transform: [{ scale: 0.98 }] },
});
