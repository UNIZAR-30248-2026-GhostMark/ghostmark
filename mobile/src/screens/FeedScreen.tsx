import { useFocusEffect } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getFeed, type FeedEntry } from "../api/game";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";
import { feedText, feedTime } from "../utils/feedTime";

type Props = NativeStackScreenProps<RootStackParamList, "Feed">;

export function FeedScreen({ navigation, route }: Props) {
  const { gameId, isPlayer } = route.params;
  const [entries, setEntries] = useState<FeedEntry[]>();
  const [error, setError] = useState<string>();
  const [attempt, setAttempt] = useState(0);

  const loadFeed = useCallback(() => {
    let active = true;
    getFeed(gameId).then((result) => {
      if (!active) return;
      setEntries(result);
      setError(undefined);
    }).catch((requestError: unknown) => {
      if (active) setError(requestError instanceof ApiError ? requestError.message : "No hemos podido cargar el feed. Comprueba la conexión e inténtalo de nuevo.");
    });
    return () => { active = false; };
  }, [gameId]);

  // Se recarga al volver al feed, por ejemplo después de una eliminación.
  useFocusEffect(loadFeed);
  useEffect(() => (attempt > 0 ? loadFeed() : undefined), [attempt, loadFeed]);

  const now = new Date();
  return <Screen padded={false}><View style={styles.screen}>
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View style={styles.tabs}>
        {isPlayer ? <Pressable accessibilityLabel="Ir a mi misión" accessibilityRole="button" onPress={() => navigation.navigate("MissionCard", { gameId })} style={styles.tab}><Text style={styles.tabText}>Misión</Text></Pressable> : <View style={styles.tab}><Text style={styles.tabText}>Misión</Text></View>}
        <View style={styles.tabOn}><Text style={styles.tabOnText}>FEED</Text></View>
        <Pressable accessibilityLabel="Ir a la pestaña Partida" accessibilityRole="button" onPress={() => navigation.navigate("GameTab", { gameId })} style={styles.tab}><Text style={styles.tabText}>Partida</Text></Pressable>
      </View>
      {error ? <View style={[styles.state, styles.errorState]}>
        <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text>
        <PrimaryButton onPress={() => { setError(undefined); setAttempt((value) => value + 1); }}>REINTENTAR</PrimaryButton>
      </View> : !entries ? <View style={styles.state}>
        <ActivityIndicator color={colors.lime} size="large" />
        <Text style={styles.loadingText}>CARGANDO FEED…</Text>
      </View> : entries.length === 0 ? <View style={styles.state}>
        <Text style={styles.empty}>Aún no hay eliminaciones.</Text>
      </View> : <View style={styles.entries}>
        {entries.map((entry, index) => <View key={`${entry.ocurridoEn}-${index}`} style={styles.entry}>
          <Image source={require("../../assets/ghosts/fantasma-ko.png")} style={styles.entryGhost} />
          <View style={styles.entryText}>
            <Text numberOfLines={2} style={styles.entryTitle}>{feedText(entry.alias)}</Text>
            <Text style={styles.entryTime}>{feedTime(entry.ocurridoEn, now)}</Text>
          </View>
        </View>)}
      </View>}
    </ScrollView>
    {/* Sin la PBI 14 no se promete el Wrapped (R11-27). */}
    <Text style={styles.footer}>El feed nunca dice quién eliminó a quién.</Text>
  </View></Screen>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 },
  content: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 18 },
  tabs: { backgroundColor: colors.ink, borderRadius: 15, flexDirection: "row", height: 49, padding: 4 },
  tab: { alignItems: "center", flex: 1, justifyContent: "center" },
  tabOn: { alignItems: "center", backgroundColor: colors.white, borderColor: "#CFC2DD", borderRadius: 11, borderWidth: 2, flex: 1, justifyContent: "center" },
  tabText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 },
  tabOnText: { color: colors.ink, ...bangers(18) },
  entries: { gap: 12, marginTop: 18, paddingBottom: 20 },
  entry: { alignItems: "center", backgroundColor: colors.coral, borderColor: colors.ink, borderRadius: 8, borderWidth: 3, boxShadow: `4px 4px 0px ${colors.border}`, flexDirection: "row", gap: 12, minHeight: 70, paddingHorizontal: 14, paddingVertical: 10 },
  entryGhost: { height: 58, marginLeft: -6, resizeMode: "contain", transform: [{ rotate: "-5deg" }], width: 62 },
  entryText: { flex: 1 },
  entryTitle: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 16 },
  entryTime: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 2 },
  state: { alignItems: "center", gap: 16, marginTop: 80 },
  errorState: { alignItems: "stretch" },
  loadingText: { color: colors.text, ...bangers(24) },
  empty: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 15, textAlign: "center" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 20, textAlign: "center" },
  footer: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 14, lineHeight: 19, paddingBottom: 24, paddingHorizontal: 30, paddingTop: 8, textAlign: "center" },
});
