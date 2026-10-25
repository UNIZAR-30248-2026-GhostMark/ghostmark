import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";

import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameStarted">;

// Como en f4-arranca: «Lun 6 oct · 9:00».
function startLabel(value: string) {
  const date = new Date(value);
  const weekday = date.toLocaleDateString("es-ES", { weekday: "short" }).replace(".", "");
  const month = date.toLocaleDateString("es-ES", { month: "short" }).replace(".", "");
  const time = `${date.getHours()}:${String(date.getMinutes()).padStart(2, "0")}`;
  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)} ${date.getDate()} ${month} · ${time}`;
}

export function GameStartedScreen({ navigation, route }: Props) {
  const { game } = route.params;
  const gameId = game.id;
  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.artwork}>
      {[-64, -38, -12, 15, 42, 69].map((rotation) => <View key={rotation} style={[styles.ray, { transform: [{ rotate: `${rotation}deg` }] }]} />)}
      <Image accessibilityLabel="Fantasma repartiendo cartas" source={require("../../assets/ghosts/fantasma-baraja.png")} style={styles.ghost} />
    </View>
    <View style={styles.content}>
      <View style={styles.dateTag}><Text style={styles.dateText}>{startLabel(game.inicioEn)}</Text></View>
      <Text style={styles.title}>¡LA PARTIDA HA{"\n"}EMPEZADO!</Text>
      <Text numberOfLines={2} style={styles.gameName}>{game.nombre} · {game.numInscritos} jugadores</Text>
      <Text style={styles.detail}>Cada jugador tiene un objetivo y una misión</Text>
    </View>
    <View style={styles.footer}>
      <Pressable accessibilityRole="button" disabled={!gameId} onPress={() => gameId && navigation.navigate("MissionCard", { gameId })} style={styles.cardButton}><Text style={styles.cardButtonText}>VER MI CARTA</Text></Pressable>
    </View>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  artwork: { alignItems: "center", backgroundColor: "#7132A8", height: "45%", justifyContent: "center", overflow: "hidden", position: "relative" },
  ray: { backgroundColor: "rgba(205,184,255,0.15)", borderRadius: 24, height: 500, left: "50%", marginLeft: -22, position: "absolute", top: -115, width: 44 },
  ghost: { height: 225, resizeMode: "contain", transform: [{ translateY: -8 }], width: 240, zIndex: 2 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24 },
  dateTag: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 7, borderWidth: 1, marginTop: -12, paddingHorizontal: 10, paddingVertical: 4 },
  dateText: { color: colors.ink, fontFamily: "ZenMaruBlack", fontSize: 10 },
  title: { color: colors.text, fontFamily: "Bangers", fontSize: 42, lineHeight: 40, marginTop: 20, textAlign: "center" },
  gameName: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 13, textAlign: "center" },
  detail: { color: colors.text, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 19, marginTop: 9, textAlign: "center" },
  footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 },
  cardButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 14, borderWidth: 3, paddingVertical: 13, shadowColor: colors.border, shadowOffset: { height: 5, width: 0 }, shadowOpacity: 1, shadowRadius: 0 },
  cardButtonText: { color: colors.ink, fontFamily: "Bangers", fontSize: 21, letterSpacing: 0.7 },
});
