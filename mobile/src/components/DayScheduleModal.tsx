import { useMemo, useState } from "react";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";

import { slotKey } from "./WeeklyScheduleGrid";
import { colors } from "../theme/colors";
import type { Weekday } from "../types/schedule";

const START_MINUTES = 8 * 60;
const END_MINUTES = 21 * 60;
const SLOT_MINUTES = 30;
const DAY_NAMES: Record<Weekday, string> = { 1: "LUNES", 2: "MARTES", 3: "MIÉRCOLES", 4: "JUEVES", 5: "VIERNES" };

type Range = { startMinutes: number; endMinutes: number };

type Props = {
  day?: Weekday;
  onClose: () => void;
  onSave: (slots: Set<string>) => void;
  selectedSlots: Set<string>;
  visible: boolean;
};

function formatTime(minutes: number) {
  return `${Math.floor(minutes / 60).toString().padStart(2, "0")}:${(minutes % 60).toString().padStart(2, "0")}`;
}

function rangesForDay(day: Weekday, selectedSlots: Set<string>): Range[] {
  const ranges: Range[] = [];
  for (let minutes = START_MINUTES; minutes < END_MINUTES; minutes += SLOT_MINUTES) {
    if (!selectedSlots.has(slotKey(day, (minutes - START_MINUTES) / SLOT_MINUTES))) continue;
    const previous = ranges.at(-1);
    if (previous?.endMinutes === minutes) previous.endMinutes += SLOT_MINUTES;
    else ranges.push({ startMinutes: minutes, endMinutes: minutes + SLOT_MINUTES });
  }
  return ranges;
}

function mergeRange(ranges: Range[], range: Range) {
  const ordered = [...ranges, range].sort((a, b) => a.startMinutes - b.startMinutes);
  return ordered.reduce<Range[]>((merged, current) => {
    const previous = merged.at(-1);
    if (previous && current.startMinutes <= previous.endMinutes) previous.endMinutes = Math.max(previous.endMinutes, current.endMinutes);
    else merged.push({ ...current });
    return merged;
  }, []);
}

export function DayScheduleModal({ day, visible, selectedSlots, onClose, onSave }: Props) {
  if (!day) return null;
  return <DayScheduleModalContent key={`${day}-${visible}-${Array.from(selectedSlots).sort().join("|")}`} day={day} onClose={onClose} onSave={onSave} selectedSlots={selectedSlots} visible={visible} />;
}

function DayScheduleModalContent({ day, visible, selectedSlots, onClose, onSave }: Omit<Props, "day"> & { day: Weekday }) {
  const initialRanges = useMemo(() => rangesForDay(day, selectedSlots), [day, selectedSlots]);
  const [ranges, setRanges] = useState<Range[]>(initialRanges);
  const [startMinutes, setStartMinutes] = useState(9 * 60);
  const [endMinutes, setEndMinutes] = useState(10 * 60);

  function changeStart(delta: number) {
    setStartMinutes((current) => {
      const next = Math.min(Math.max(START_MINUTES, current + delta), END_MINUTES - SLOT_MINUTES);
      setEndMinutes((end) => Math.max(end, next + SLOT_MINUTES));
      return next;
    });
  }

  function changeEnd(delta: number) {
    setEndMinutes((current) => {
      const next = Math.min(Math.max(START_MINUTES + SLOT_MINUTES, current + delta), END_MINUTES);
      setStartMinutes((start) => Math.min(start, next - SLOT_MINUTES));
      return next;
    });
  }

  function saveDay() {
    const next = new Set(selectedSlots);
    for (let minutes = START_MINUTES; minutes < END_MINUTES; minutes += SLOT_MINUTES) next.delete(slotKey(day, (minutes - START_MINUTES) / SLOT_MINUTES));
    for (const range of ranges) for (let minutes = range.startMinutes; minutes < range.endMinutes; minutes += SLOT_MINUTES) next.add(slotKey(day, (minutes - START_MINUTES) / SLOT_MINUTES));
    onSave(next);
  }

  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <View style={styles.overlay}>
        <Pressable onPress={onClose} style={styles.backdrop} />
        <View style={styles.modal}>
          <View style={styles.header}>
            <View><Text style={styles.kicker}>HORARIO LECTIVO</Text><Text style={styles.title}>{DAY_NAMES[day]}</Text></View>
            <Pressable accessibilityRole="button" onPress={onClose} style={styles.closeButton}><Text style={styles.closeText}>×</Text></Pressable>
          </View>
          <Text style={styles.description}>Añade las horas en las que tienes clase</Text>
          <View style={styles.timeSection}>
            <TimeControl label="DESDE" onDecrease={() => changeStart(-SLOT_MINUTES)} onIncrease={() => changeStart(SLOT_MINUTES)} value={formatTime(startMinutes)} />
            <TimeControl label="HASTA" onDecrease={() => changeEnd(-SLOT_MINUTES)} onIncrease={() => changeEnd(SLOT_MINUTES)} value={formatTime(endMinutes)} />
          </View>
          <Pressable accessibilityRole="button" onPress={() => setRanges((current) => mergeRange(current, { startMinutes, endMinutes }))} style={styles.addButton}><Text style={styles.addText}>+ AÑADIR FRANJA</Text></Pressable>
          <View style={styles.rangesSection}>
            <Text style={styles.rangesTitle}>TUS CLASES</Text>
            {ranges.length === 0 ? <Text style={styles.emptyText}>Todavía no has añadido ninguna franja</Text> : ranges.map((range, index) => (
              <View key={`${range.startMinutes}-${range.endMinutes}`} style={styles.range}>
                <Text style={styles.rangeText}>{formatTime(range.startMinutes)} — {formatTime(range.endMinutes)}</Text>
                <Pressable accessibilityLabel="Eliminar franja" accessibilityRole="button" onPress={() => setRanges((current) => current.filter((_, currentIndex) => currentIndex !== index))}><Text style={styles.removeText}>×</Text></Pressable>
              </View>
            ))}
          </View>
          <Pressable accessibilityRole="button" onPress={saveDay} style={styles.saveButton}><Text style={styles.saveText}>GUARDAR DÍA</Text></Pressable>
        </View>
      </View>
    </Modal>
  );
}

function TimeControl({ label, value, onDecrease, onIncrease }: { label: string; value: string; onDecrease: () => void; onIncrease: () => void }) {
  return <View style={styles.timeControlGroup}><Text style={styles.timeLabel}>{label}</Text><View style={styles.timeControl}><Pressable accessibilityLabel={`Reducir ${label}`} accessibilityRole="button" onPress={onDecrease} style={styles.timeButton}><Text style={styles.timeButtonText}>−</Text></Pressable><Text style={styles.timeValue}>{value}</Text><Pressable accessibilityLabel={`Aumentar ${label}`} accessibilityRole="button" onPress={onIncrease} style={styles.timeButton}><Text style={styles.timeButtonText}>+</Text></Pressable></View></View>;
}

const styles = StyleSheet.create({
  overlay: { alignItems: "center", flex: 1, justifyContent: "center", paddingHorizontal: 24 },
  backdrop: { backgroundColor: "rgba(8, 2, 15, 0.78)", bottom: 0, left: 0, position: "absolute", right: 0, top: 0 },
  modal: { backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 22, borderWidth: 3, padding: 20, shadowColor: colors.ink, shadowOffset: { width: 0, height: 7 }, shadowOpacity: 1, shadowRadius: 0, width: "100%" },
  header: { alignItems: "flex-start", flexDirection: "row", justifyContent: "space-between" },
  kicker: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 12, letterSpacing: 1 },
  title: { color: colors.text, fontFamily: "Bangers", fontSize: 34, letterSpacing: 1, marginTop: 2 },
  closeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 18, borderWidth: 2, height: 36, justifyContent: "center", width: 36 },
  closeText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 22, lineHeight: 24 },
  description: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 14, marginTop: 8 },
  timeSection: { flexDirection: "row", gap: 12, marginTop: 18 },
  timeControlGroup: { flex: 1, gap: 7 }, timeLabel: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 12, textAlign: "center" },
  timeControl: { alignItems: "center", backgroundColor: colors.background, borderColor: colors.border, borderRadius: 14, borderWidth: 2, flexDirection: "row", justifyContent: "space-between", padding: 5 },
  timeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderRadius: 9, height: 32, justifyContent: "center", width: 32 }, timeButtonText: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 20, lineHeight: 22 }, timeValue: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 14 },
  addButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 13, borderWidth: 2, marginTop: 14, paddingVertical: 10 }, addText: { color: colors.ink, fontFamily: "Bangers", fontSize: 17, letterSpacing: 0.8 },
  rangesSection: { gap: 6, marginTop: 16 }, rangesTitle: { color: colors.text, fontFamily: "Bangers", fontSize: 19, letterSpacing: 0.7 }, emptyText: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, textAlign: "center" },
  range: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 2, flexDirection: "row", justifyContent: "space-between", paddingHorizontal: 12, paddingVertical: 6 }, rangeText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 12 }, removeText: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 17, lineHeight: 18 },
  saveButton: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 16, borderWidth: 3, marginTop: 18, paddingVertical: 11, shadowColor: colors.border, shadowOffset: { width: 0, height: 5 }, shadowOpacity: 1, shadowRadius: 0 }, saveText: { color: colors.ink, fontFamily: "Bangers", fontSize: 19, letterSpacing: 0.8 },
});
