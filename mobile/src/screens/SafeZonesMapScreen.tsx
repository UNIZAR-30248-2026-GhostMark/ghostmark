import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

import { MapZones } from "../components/MapZones";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "SafeZonesMap">;

/** Mapa separado del scroll de Partida para que Leaflet reciba todos los gestos. */
export function SafeZonesMapScreen({ navigation, route }: Props) {
  const { height } = useWindowDimensions();
  return <Screen padded={false}><View style={styles.screen}>
    <View style={styles.header}><Text style={styles.title}>ZONAS SEGURAS</Text><Pressable accessibilityLabel="Volver a partida" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.close}><Text style={styles.closeText}>CERRAR</Text></Pressable></View>
    <Text style={styles.help}>Mueve y amplía el mapa con dos dedos.</Text>
    <MapZones height={Math.max(330, height - 172)} zones={route.params.zones} />
    <View style={styles.note}><Text style={styles.shield}>⌾</Text><Text style={styles.noteText}>No se elimina dentro de una zona. Nadie ve dónde estás.</Text></View>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1, paddingHorizontal: 19, paddingTop: 13 },
  header: { alignItems: "center", flexDirection: "row", justifyContent: "space-between" },
  title: { color: colors.text, ...bangers(28), flex: 1, lineHeight: 34 },
  close: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 9, borderWidth: 2, justifyContent: "center", minHeight: 39, paddingHorizontal: 12 },
  closeText: { color: colors.ink, ...bangers(14), lineHeight: 18 },
  help: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 12, marginBottom: 13, marginTop: 7, textAlign: "center" },
  note: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", gap: 8, marginTop: 13, minHeight: 58, paddingHorizontal: 12 },
  shield: { color: colors.lime, fontSize: 23 },
  noteText: { color: colors.text, flex: 1, fontFamily: "ZenMaruBold", fontSize: 11, lineHeight: 16 },
});
