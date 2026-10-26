import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { joinGame } from "../api/inscription";

import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameNorms">;

const rules = [
  { color: colors.danger, icon: "hand-left-outline" as const, label: "Sin contacto físico" },
  { color: colors.lime, icon: "ban-outline" as const, label: "Sin objetos que simulen armas" },
  { color: "#CDB8FF", icon: "shield-checkmark-outline" as const, label: "Zonas y horarios seguros" },
  { color: colors.lime, icon: "exit-outline" as const, label: "Puedes abandonar cuando quieras" },
];

function formatDates(start: string, end: string) {
  const options: Intl.DateTimeFormatOptions = { day: "numeric", month: "short" };
  const startLabel = new Date(start).toLocaleDateString("es-ES", options);
  const endLabel = new Date(end).toLocaleDateString("es-ES", options);
  return `${startLabel} – ${endLabel}`;
}

function memberCountLabel(count: number) {
  return `${count} ${count === 1 ? "inscrito" : "inscritos"}`;
}

export function GameNormsScreen({ navigation, route }: Props) {
  const { code, game } = route.params;
  const [isJoining, setIsJoining] = useState(false);
  const [error, setError] = useState<string>();
  const [alreadyIn, setAlreadyIn] = useState(false);

  async function join() {
    setError(undefined);
    setIsJoining(true);
    try {
      const inscription = await joinGame(code);
      navigation.replace("WaitingRoom", { gameId: inscription.partidaId, game });
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.code === "YA_INSCRITO") setAlreadyIn(true);
      else setError(requestError instanceof ApiError ? requestError.message : "No hemos podido unirte a la partida. Comprueba la conexión e inténtalo de nuevo.");
    } finally {
      setIsJoining(false);
    }
  }

  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.content}>
      <Pressable accessibilityLabel="Volver a la invitación" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.back}>
        <Ionicons color={colors.text} name="arrow-back" size={23} />
      </Pressable>
      <View style={styles.details}>
        <Text numberOfLines={2} style={styles.gameTitle}>{game.nombre.toUpperCase()}</Text>
        <Text style={styles.subtitle}>{formatDates(game.inicioEn, game.finEn)} · {memberCountLabel(game.numInscritos)}</Text>
        <Text style={styles.heading}>NORMAS DE LA PARTIDA</Text>
        <View style={styles.rules}>
          {rules.map((rule) => <View key={rule.label} style={[styles.rule, { backgroundColor: rule.color }]}>
            <Ionicons color={colors.ink} name={rule.icon} size={23} />
            <Text style={styles.ruleText}>{rule.label}</Text>
          </View>)}
        </View>
      </View>
    </View>
    <View style={styles.footer}>
      {alreadyIn ? <Text accessibilityLiveRegion="polite" style={styles.notice}>Ya estás inscrito en esta partida.</Text> : null}
      {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
      {alreadyIn
        ? <PrimaryButton onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}>IR A MIS PARTIDAS</PrimaryButton>
        : <PrimaryButton disabled={isJoining} onPress={join}>{isJoining ? "UNIÉNDOTE…" : "ACEPTO Y ME UNO"}</PrimaryButton>}
    </View>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 12 },
  back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, height: 44, justifyContent: "center", width: 44 },
  details: { flex: 1, paddingTop: 16 },
  gameTitle: { ...typography.title, color: colors.text, fontSize: 32, lineHeight: 35 },
  subtitle: { ...typography.body, color: colors.textMuted, fontSize: 14, marginTop: 7 },
  heading: { ...typography.title, color: colors.text, fontSize: 25, marginTop: 25 },
  rules: { gap: 12, marginTop: 16 },
  rule: { alignItems: "center", borderColor: colors.ink, borderRadius: 16, borderWidth: 3, flexDirection: "row", gap: 13, minHeight: 55, paddingHorizontal: 15, shadowColor: colors.ink, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 0.75, shadowRadius: 0 },
  ruleText: { color: colors.ink, flex: 1, fontFamily: "ZenMaruBold", fontSize: 16, lineHeight: 21 },
  footer: { gap: 10, paddingBottom: 14, paddingHorizontal: 24, paddingTop: 8 },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, textAlign: "center" },
  notice: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 15, lineHeight: 21, textAlign: "center" },
});
