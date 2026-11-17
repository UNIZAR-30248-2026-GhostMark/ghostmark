import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Image, Modal, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useState } from "react";

import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";
import { formatGameDate } from "../utils/gameDate";
import { resultParts, winnerNames } from "../utils/gameResult";

type Props = NativeStackScreenProps<RootStackParamList, "GameEnded">;

export function GameEndedScreen({ navigation, route }: Props) {
  const { game } = route.params;
  const result = game.resultado ? resultParts(game.resultado) : undefined;
  const winners = game.resultado ? winnerNames(game.resultado) : [];
  const hasLongTie = game.resultado?.tipo === "EMPATE" && winners.length > 3;
  const [showWinners, setShowWinners] = useState(false);
  const closedAt = game.resultado?.cerradaEn ?? game.finEn;
  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.artwork}>{[-50, -25, 0, 25, 50].map((rotation) => <View key={rotation} style={[styles.ray, { transform: [{ rotate: `${rotation}deg` }] }]} />)}<Image accessibilityLabel="Fantasma celebrando el final" source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.ghost} /></View>
    <View style={styles.content}><View style={styles.date}><Text style={styles.dateText}>{formatGameDate(closedAt)}</Text></View><Text style={styles.title}>LA PARTIDA HA{"\n"}TERMINADO</Text><Text style={styles.details}>{game.nombre} · {game.numInscritos} jugadores · {durationText(game.inicioEn, closedAt)}</Text>{result ? <Pressable accessibilityLabel={hasLongTie ? "Ver lista completa del empate" : undefined} accessibilityRole={hasLongTie ? "button" : undefined} disabled={!hasLongTie} onPress={() => setShowWinners(true)} style={styles.result}><Text adjustsFontSizeToFit minimumFontScale={0.6} numberOfLines={2} style={styles.resultTitle}>{result.title.toUpperCase()}</Text><Text numberOfLines={2} style={styles.resultDetail}>{result.detail}</Text></Pressable> : null}</View>
    <View style={styles.footer}><Pressable accessibilityRole="button" onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })} style={({ pressed }) => [styles.backButton, pressed && styles.pressed]}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.backButtonText}>VOLVER A MIS PARTIDAS</Text></Pressable></View>
    {showWinners ? <Modal animationType="fade" onRequestClose={() => setShowWinners(false)} transparent visible><View style={styles.modalOverlay}><View style={styles.modal}><Text style={styles.modalTitle}>EMPATE ENTRE</Text><ScrollView contentContainerStyle={styles.winnerList}>{winners.map((winner, index) => <Text key={`${winner}-${index}`} style={styles.winner}>{winner}</Text>)}</ScrollView><Pressable accessibilityRole="button" onPress={() => setShowWinners(false)} style={styles.closeModal}><Text style={styles.closeModalText}>CERRAR</Text></Pressable></View></View></Modal> : null}
  </View></Screen>;
}

function durationText(start: string, end: string) {
  const durationMs = Math.max(0, new Date(end).getTime() - new Date(start).getTime());
  const days = Math.max(1, Math.ceil(durationMs / 86_400_000));
  return `${days} ${days === 1 ? "día" : "días"}`;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, artwork: { alignItems: "center", backgroundColor: colors.limePale, height: "46%", justifyContent: "center", overflow: "hidden" }, ray: { backgroundColor: "rgba(112,177,51,0.13)", height: 520, position: "absolute", top: -150, width: 66 }, ghost: { height: 235, resizeMode: "contain", width: 245, zIndex: 1 }, content: { alignItems: "center", flex: 1, paddingHorizontal: 24 }, date: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, marginTop: -14, paddingHorizontal: 10, paddingVertical: 4 }, dateText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 11 }, title: { color: colors.text, ...bangers(42), lineHeight: 50, marginTop: 21, textAlign: "center" }, details: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, marginTop: 17, textAlign: "center" }, footer: { paddingBottom: 12, paddingHorizontal: 24, paddingTop: 8 }, backButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 2, boxShadow: `0px 5px 0px ${colors.border}`, justifyContent: "center", minHeight: 52 }, backButtonText: { alignSelf: "stretch", color: colors.ink, ...bangers(22), letterSpacing: 0.8, textAlign: "center" }, pressed: { opacity: 0.72 },
  result: { alignSelf: "stretch", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 16, borderWidth: 3, boxShadow: `0px 5px 0px ${colors.border}`, marginTop: 20, paddingHorizontal: 16, paddingVertical: 14 },
  resultTitle: { color: colors.text, ...bangers(30), textAlign: "center" },
  resultDetail: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 15, marginTop: 4, textAlign: "center" },
  modalOverlay: { alignItems: "center", backgroundColor: "rgba(20,7,31,0.72)", flex: 1, justifyContent: "center", padding: 28 },
  modal: { backgroundColor: colors.surfaceRaised, borderColor: colors.ink, borderRadius: 18, borderWidth: 3, maxHeight: "72%", padding: 20, width: "100%" },
  modalTitle: { color: colors.text, ...bangers(30), textAlign: "center" },
  winnerList: { gap: 8, paddingVertical: 18 },
  winner: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 16, textAlign: "center" },
  closeModal: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, minHeight: 42, justifyContent: "center" },
  closeModalText: { color: colors.ink, ...bangers(19) },
});
