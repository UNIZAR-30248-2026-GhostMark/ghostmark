import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useMemo, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { ApiError } from "../api/client";
import { getGameSchedule, updateGameSchedule } from "../api/inscription";
import { getSchedule, updateSchedule } from "../api/schedule";
import { DayScheduleModal } from "../components/DayScheduleModal";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { scheduleToSelectedSlots, selectedSlotsToSchedule, WeeklyScheduleGrid } from "../components/WeeklyScheduleGrid";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { RootStackParamList } from "../types/navigation";
import type { Weekday } from "../types/schedule";

type Props = NativeStackScreenProps<RootStackParamList, "Schedule">;

export function ScheduleScreen({ navigation, route }: Props) {
  const [selectedSlots, setSelectedSlots] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [editingDay, setEditingDay] = useState<Weekday>();
  const gameId = route.params?.gameId;
  const schedule = useMemo(() => selectedSlotsToSchedule(selectedSlots), [selectedSlots]);

  useEffect(() => {
    let isMounted = true;

    async function loadSchedule() {
      try {
        const savedSchedule = gameId ? await getGameSchedule(gameId) : await getSchedule();
        if (isMounted) setSelectedSlots(scheduleToSelectedSlots(savedSchedule));
      } catch {
        if (isMounted) setError("No hemos podido cargar tu horario. Puedes volver a marcarlo y guardarlo.");
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void loadSchedule();
    return () => { isMounted = false; };
  }, [gameId]);

  async function save() {
    setError(undefined);
    setIsSaving(true);
    try {
      if (gameId) await updateGameSchedule(gameId, schedule);
      else await updateSchedule(schedule);
      if (route.params?.onboarding) navigation.replace("LocationConsent");
      else if (navigation.canGoBack()) navigation.goBack();
      else navigation.replace("Home");
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.code === "PARTIDA_NO_EDITABLE") {
        setError("La partida ya ha empezado: tu horario queda fijo para toda la partida.");
      } else {
        setError("No hemos podido guardar tu horario. Comprueba la conexión e inténtalo de nuevo.");
      }
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Screen padded={false}>
      <View style={styles.content}>
        <View style={styles.header}>
          {route.params?.onboarding ? <View style={styles.dots}><View style={styles.dot} /><View style={[styles.dot, styles.dotOn]} /><View style={styles.dot} /><View style={styles.dot} /></View> : null}
          <Text style={styles.title}>TU HORARIO LECTIVO</Text>
          <Text style={styles.description}>En estas horas nadie podrá cazarte, y tú tampoco podrás cazar</Text>
        </View>
        <View style={styles.gridArea}>
          <WeeklyScheduleGrid onEditDay={setEditingDay} selectedSlots={selectedSlots} />
          <Text style={styles.helper}>Toca L, M, X, J o V para indicar las clases de cada día</Text>
        </View>
        {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
        <View style={styles.spacer} />
        <PrimaryButton disabled={isSaving || isLoading} onPress={save}>{isLoading ? "CARGANDO…" : isSaving ? "GUARDANDO…" : route.params?.onboarding ? "SIGUIENTE" : "CONFIRMAR HORARIO"}</PrimaryButton>
      </View>
      <DayScheduleModal day={editingDay} onClose={() => setEditingDay(undefined)} onSave={(slots) => { setSelectedSlots(slots); setEditingDay(undefined); }} selectedSlots={selectedSlots} visible={editingDay !== undefined} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flex: 1, paddingBottom: 14, paddingHorizontal: 24, paddingTop: 14 },
  header: { gap: 8 },
  dots: { flexDirection: "row", gap: 7, marginBottom: 7 },
  dot: { backgroundColor: colors.surfaceRaised, borderColor: colors.ink, borderRadius: 8, borderWidth: 2, height: 9, width: 21 },
  dotOn: { backgroundColor: colors.lime, width: 39 },
  title: { ...typography.title, fontSize: 32, lineHeight: 36 },
  description: { ...typography.body, color: colors.textMuted, fontSize: 15, lineHeight: 21 },
  gridArea: { gap: 10, marginTop: 14 },
  helper: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13, textAlign: "center" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 16, marginTop: 9, textAlign: "center" },
  spacer: { flex: 1, minHeight: 12 },
});
