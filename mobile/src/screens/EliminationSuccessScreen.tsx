import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";

import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "EliminationSuccess">;

export function EliminationSuccessScreen({ navigation, route }: Props) {
  const { gameId, result } = route.params;
  const inheritedTarget = result.carta.aliasObjetivo ?? "NADIE";

  return <Screen padded={false}><View style={styles.screen}><View style={styles.content}>
    <Image accessibilityLabel="Fantasma celebrando el acierto" source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.ghost} />
    <Text style={styles.title}>¡ACIERTO!</Text>
    <Text style={styles.text}>Has eliminado a {result.aliasVictima ?? "cuenta eliminada"}.</Text>
    {result.carta.aliasObjetivo ? <View style={styles.inherited}><Text style={styles.inheritedLabel}>TU NUEVO OBJETIVO</Text><Text adjustsFontSizeToFit minimumFontScale={0.5} numberOfLines={1} style={styles.target}>{inheritedTarget.toUpperCase()}</Text></View> : <Text style={styles.lastPlayer}>Ya no tienes objetivo. ¡Eres la última persona en juego!</Text>}
  </View><View style={styles.footer}><Pressable accessibilityRole="button" onPress={() => navigation.replace("MissionCard", { gameId })} style={styles.button}><Text style={styles.buttonText}>VER MI CARTA</Text></Pressable></View></View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { alignItems: "center", flex: 1, justifyContent: "center", paddingHorizontal: 28 },
  ghost: { height: 205, resizeMode: "contain", width: 220 },
  title: { color: colors.lime, ...bangers(52), letterSpacing: 1, marginTop: 12, textShadowColor: colors.ink, textShadowOffset: { height: 3, width: 1 }, textShadowRadius: 0 },
  text: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16, lineHeight: 23, marginTop: 16, textAlign: "center" },
  inherited: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 17, borderWidth: 3, marginTop: 24, paddingHorizontal: 18, paddingVertical: 14, width: "100%" },
  inheritedLabel: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 11, letterSpacing: 1 }, target: { alignSelf: "stretch", color: colors.text, ...bangers(32), marginTop: 6, textAlign: "center" },
  lastPlayer: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 14, lineHeight: 21, marginTop: 22, textAlign: "center" },
  footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 },
  button: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 15, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.ink}`, minHeight: 52, justifyContent: "center" },
  buttonText: { alignSelf: "stretch", color: colors.ink, ...bangers(22), textAlign: "center" },
});
