import { useMemo } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { colors } from "../theme/colors";
import type { Schedule, Weekday } from "../types/schedule";

const DAYS: readonly { day: Weekday; label: string }[] = [
  { day: 1, label: "L" },
  { day: 2, label: "M" },
  { day: 3, label: "X" },
  { day: 4, label: "J" },
  { day: 5, label: "V" },
];

const START_HOUR = 8;
const SLOT_COUNT = 26;
const HOURS = [8, 10, 12, 14, 16, 18, 20];

export function slotKey(day: Weekday, slot: number) {
  return `${day}-${slot}`;
}

function slotTime(slot: number) {
  const totalMinutes = START_HOUR * 60 + slot * 30;
  const hour = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${hour.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}`;
}

export function selectedSlotsToSchedule(selectedSlots: Set<string>): Schedule {
  return {
    franjas: DAYS.flatMap(({ day }) =>
      Array.from({ length: SLOT_COUNT }, (_, slot) => slot)
        .filter((slot) => selectedSlots.has(slotKey(day, slot)))
        .map((slot) => ({ dia: day, horaInicio: slotTime(slot) })),
    ),
  };
}

export function scheduleToSelectedSlots(schedule: Schedule) {
  const selectedSlots = new Set<string>();

  schedule.franjas.forEach(({ dia, horaInicio }) => {
    const [hour, minute] = horaInicio.split(":").map(Number);
    if (hour === undefined || minute === undefined) return;
    const slot = (hour - START_HOUR) * 2 + minute / 30;

    if (Number.isInteger(slot) && slot >= 0 && slot < SLOT_COUNT) {
      selectedSlots.add(slotKey(dia, slot));
    }
  });

  return selectedSlots;
}

type Props = {
  selectedSlots: Set<string>;
  onEditDay: (day: Weekday) => void;
};

export function WeeklyScheduleGrid({ selectedSlots, onEditDay }: Props) {
  const slots = useMemo(() => Array.from({ length: SLOT_COUNT }, (_, slot) => slot), []);

  return (
    <View style={styles.wrapper}>
      <View style={styles.hours}>
        {HOURS.map((hour) => <Text key={hour} style={styles.hour}>{`${hour}:00`}</Text>)}
      </View>
      <View style={styles.gridShell}>
        <View style={styles.dayLabels}>
          {DAYS.map(({ day, label }) => (
            <Pressable accessibilityRole="button" key={day} onPress={() => onEditDay(day)} style={({ pressed }) => [styles.dayButton, pressed && styles.dayButtonPressed]}>
              <Text style={styles.day}>{label}</Text>
            </Pressable>
          ))}
        </View>
        <View style={styles.grid}>
          {slots.map((slot) => (
            <View key={slot} style={styles.row}>
              {DAYS.map(({ day }) => {
                const selected = selectedSlots.has(slotKey(day, slot));
                const previousSelected = slot > 0 && selectedSlots.has(slotKey(day, slot - 1));
                const nextSelected = slot < SLOT_COUNT - 1 && selectedSlots.has(slotKey(day, slot + 1));
                return (
                  <View
                    key={day}
                    style={[
                      styles.cell,
                      selected && styles.selectedCell,
                      selected && previousSelected && styles.selectedConnectedTop,
                      selected && nextSelected && styles.selectedConnectedBottom,
                    ]}
                  />
                );
              })}
            </View>
          ))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { alignItems: "stretch", flexDirection: "row", gap: 7 },
  hours: { height: 347, justifyContent: "space-between", paddingBottom: 2, paddingTop: 32, width: 39 },
  hour: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 10 },
  gridShell: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 13, borderWidth: 3, flex: 1, paddingBottom: 9, paddingHorizontal: 8, paddingTop: 7, shadowColor: colors.border, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 1, shadowRadius: 0 },
  dayLabels: { flexDirection: "row", gap: 4, height: 27 },
  dayButton: { alignItems: "center", borderRadius: 7, flex: 1, justifyContent: "center" },
  dayButtonPressed: { backgroundColor: colors.border },
  day: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 14, textAlign: "center" },
  grid: { height: 305 },
  row: { flex: 1, flexDirection: "row", gap: 4 },
  cell: { backgroundColor: "#3A2360", borderRadius: 3, flex: 1, marginBottom: 1 },
  selectedCell: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 7, borderWidth: 2, marginBottom: 0 },
  selectedConnectedTop: { borderTopLeftRadius: 0, borderTopRightRadius: 0, borderTopWidth: 0 },
  selectedConnectedBottom: { borderBottomLeftRadius: 0, borderBottomRightRadius: 0, borderBottomWidth: 0 },
});
