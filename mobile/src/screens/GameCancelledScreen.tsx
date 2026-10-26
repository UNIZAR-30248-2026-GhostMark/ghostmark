import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Image, StyleSheet, Text, View } from "react-native";

import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "GameCancelled">;

const MIN_PLAYERS = 3;

export function GameCancelledScreen({ navigation, route }: Props) {
  const { game } = route.params;
  return <Screen><View style={styles.screen}>
    <Image accessibilityLabel="Fantasma despidiéndose" source={require("../../assets/ghosts/fantasma-chao.png")} style={styles.ghost} />
    <View style={styles.content}>
      <View style={styles.tag}><Text style={styles.tagText}>Cancelada</Text></View>
      {game.motivoCancelacion === "POCOS_JUGADORES" ? <>
        <Text style={styles.title}>NO HAY JUGADORES{"\n"}SUFICIENTES</Text>
        <Text numberOfLines={2} style={styles.gameName}>{game.nombre}</Text>
        <View style={styles.counts}>
          <View style={styles.count}><Text style={[styles.countValue, styles.countMissing]}>{game.numInscritos}</Text><Text style={styles.countLabel}>inscritos</Text></View>
          <View style={styles.count}><Text style={[styles.countValue, styles.countNeeded]}>{MIN_PLAYERS}</Text><Text style={styles.countLabel}>hacen falta</Text></View>
        </View>
      </> : <>
        <Text style={styles.title}>PARTIDA{"\n"}CANCELADA</Text>
        <Text numberOfLines={2} style={styles.gameName}>{game.nombre}</Text>
        <Text style={styles.detail}>{game.motivoCancelacion === "MASTER_SIN_CUENTA" ? "El máster ha borrado su cuenta." : "Esta partida ya no está disponible."}</Text>
      </>}
    </View>
    <PrimaryButton onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}>VOLVER A MIS PARTIDAS</PrimaryButton>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  ghost: { alignSelf: "center", height: 205, marginTop: 18, resizeMode: "contain", width: 210 },
  content: { alignItems: "center", flex: 1, paddingHorizontal: 24, paddingTop: 10 },
  tag: { backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 8, borderWidth: 1, paddingHorizontal: 12, paddingVertical: 5 },
  tagText: { color: colors.ink, fontFamily: "ZenMaruBlack", fontSize: 11 },
  title: { color: colors.text, fontFamily: "Bangers", fontSize: 38, lineHeight: 39, marginTop: 18, textAlign: "center", width: "100%" },
  gameName: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 15, marginTop: 14, textAlign: "center" },
  detail: { color: colors.text, fontFamily: "ZenMaru", fontSize: 14, lineHeight: 20, marginTop: 17, textAlign: "center" },
  counts: { alignSelf: "stretch", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 14, borderWidth: 2, flexDirection: "row", marginTop: 22, paddingVertical: 16 },
  count: { alignItems: "center", flex: 1 },
  countValue: { fontFamily: "Bangers", fontSize: 40, lineHeight: 42 },
  countMissing: { color: colors.danger },
  countNeeded: { color: colors.lime },
  countLabel: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 4 },
});
