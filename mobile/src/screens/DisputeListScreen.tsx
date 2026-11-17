import { Ionicons } from "@expo/vector-icons";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getDisputes } from "../api/game";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { Dispute } from "../types/game";
import type { RootStackParamList } from "../types/navigation";
import { disputeListMeta } from "../utils/dispute";

type Props = NativeStackScreenProps<RootStackParamList, "DisputeList">;

export function DisputeListScreen({ navigation, route }: Props) {
  const [items, setItems] = useState<Dispute[]>(); const [error, setError] = useState<string>();
  useEffect(() => {
    let active = true;
    getDisputes(route.params.gameId).then((value) => { if (active) setItems(value); }).catch((reason: unknown) => { if (active) setError(reason instanceof ApiError ? reason.message : "No hemos podido cargar tus disputas."); });
    return () => { active = false; };
  }, [route.params.gameId]);
  return <Screen><View style={styles.screen}>
    <View style={styles.header}><Pressable accessibilityLabel="Volver" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.back}><Ionicons color={colors.text} name="arrow-back" size={23} /></Pressable><Text style={styles.title}>MIS DISPUTAS</Text></View>
    <Text style={styles.hint}>Solo aparecen las disputas en las que participas o arbitras.</Text>
    {error ? <Text style={styles.error}>{error}</Text> : !items ? <ActivityIndicator color={colors.lime} size="large" style={styles.loading} /> : items.length === 0 ? <Text style={styles.empty}>No tienes disputas pendientes.</Text> : items.map((item) => <Pressable accessibilityRole="button" key={item.id} onPress={() => navigation.navigate("Dispute", { gameId: route.params.gameId, disputeId: item.id })} style={styles.item}><Text style={styles.itemTitle}>{item.mision.toUpperCase()}</Text><Text style={styles.itemMeta}>{disputeListMeta(item)}</Text></Pressable>)}
  </View></Screen>;
}
const styles = StyleSheet.create({
  screen: { flex: 1 }, header: { alignItems: "center", flexDirection: "row", gap: 13 }, back: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 3, height: 44, justifyContent: "center", width: 44 }, title: { color: colors.text, ...bangers(29), lineHeight: 37, paddingTop: 4 }, hint: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, lineHeight: 19, marginTop: 21, textAlign: "center" }, loading: { marginTop: 70 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", marginTop: 50, textAlign: "center" }, empty: { color: colors.textMuted, fontFamily: "ZenMaruBold", marginTop: 60, textAlign: "center" }, item: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 14, borderWidth: 3, marginTop: 24, padding: 16 }, itemTitle: { color: colors.text, ...bangers(21), lineHeight: 28, paddingTop: 3 }, itemMeta: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 11, marginTop: 9 },
});
