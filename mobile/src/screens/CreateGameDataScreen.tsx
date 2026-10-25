import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useState } from "react";
import { KeyboardAvoidingView, Modal, Platform, Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import { typography } from "../theme/typography";
import type { GameDraft } from "../types/game";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "CreateGameData">;
type Editing = "start" | "end";
const formatDay = (date: Date) => date.toLocaleDateString("es-ES", { day: "numeric", month: "short", weekday: "short" }).replace(",", "");
const toTime = (date: Date) => `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
function setTime(date: Date, time: string) { const [hour = 0, minute = 0] = time.split(":").map(Number); const result = new Date(date); result.setHours(hour, minute, 0, 0); return result; }
function moveTime(time: string, delta: number) { const [hour = 0, minute = 0] = time.split(":").map(Number); const value = (hour * 60 + minute + delta + 1440) % 1440; return `${Math.floor(value / 60).toString().padStart(2, "0")}:${(value % 60).toString().padStart(2, "0")}`; }
function moveDate(date: Date, delta: number) { const result = new Date(date); result.setDate(result.getDate() + delta); return result; }

export function CreateGameDataScreen({ navigation }: Props) {
  const [startDate, setStartDate] = useState(() => { const date = new Date(); date.setDate(date.getDate() + 1); date.setHours(9, 0, 0, 0); return date; });
  const [endDate, setEndDate] = useState(() => { const date = new Date(); date.setDate(date.getDate() + 5); date.setHours(21, 0, 0, 0); return date; });
  const [name, setName] = useState("Killer Bienvenida EINA");
  const [editing, setEditing] = useState<Editing>();
  const [error, setError] = useState<string>();
  const selectedDate = editing === "start" ? startDate : endDate;
  const selectedTime = toTime(selectedDate);

  function updateSelectedDate(change: (date: Date) => Date) { if (editing === "start") setStartDate(change); if (editing === "end") setEndDate(change); }
  function continueToRules() {
    const start = startDate; const end = endDate;
    if (name.trim().length < 3) { setError("El nombre de la partida debe tener al menos 3 caracteres."); return; }
    if (start.getTime() < Date.now() + 15 * 60 * 1000) { setError("El inicio debe ser al menos 15 minutos en el futuro."); return; }
    if (end <= start) { setError("La partida debe terminar después de empezar."); return; }
    const game: GameDraft = { nombre: name.trim(), inicioEn: start.toISOString(), finEn: end.toISOString() };
    navigation.navigate("CreateGameRules", { endLabel: `${formatDay(end)} · ${toTime(end)}`, game, startLabel: `${formatDay(start)} · ${toTime(start)}` });
  }

  return <Screen><KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"} style={styles.screen}><GameWizardHeader label="Datos" onBack={() => navigation.goBack()} step={1} /><View style={styles.content}><Text style={styles.heading}>¿CÓMO SE LLAMA Y CUÁNDO{"\n"}SE JUEGA?</Text><Text style={styles.label}>Nombre</Text><TextInput accessibilityLabel="Nombre de la partida" maxLength={40} onChangeText={setName} placeholder="Nombre de la partida" placeholderTextColor={colors.textMuted} style={styles.input} value={name} /><View style={styles.dateRow}><DateCard date={startDate} label="Empieza" onPress={() => setEditing("start")} /><DateCard date={endDate} label="Termina" onPress={() => setEditing("end")} /></View>{error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}</View><PrimaryButton onPress={continueToRules}>SIGUIENTE</PrimaryButton></KeyboardAvoidingView><Modal animationType="fade" onRequestClose={() => setEditing(undefined)} transparent visible={editing !== undefined}><View style={styles.overlay}><Pressable onPress={() => setEditing(undefined)} style={styles.backdrop} /><View style={styles.modal}><Text style={styles.modalTitle}>{editing === "start" ? "INICIO DE PARTIDA" : "FIN DE PARTIDA"}</Text><Text style={styles.modalSubtitle}>Ajusta el día y la hora</Text><PickerRow label="DÍA" value={formatDay(selectedDate)} onMinus={() => updateSelectedDate((date) => moveDate(date, -1))} onPlus={() => updateSelectedDate((date) => moveDate(date, 1))} /><PickerRow label="HORA" value={selectedTime} onMinus={() => updateSelectedDate((date) => setTime(date, moveTime(toTime(date), -30)))} onPlus={() => updateSelectedDate((date) => setTime(date, moveTime(toTime(date), 30)))} /><Pressable accessibilityRole="button" onPress={() => setEditing(undefined)} style={styles.doneButton}><Text style={styles.doneText}>LISTO</Text></Pressable></View></View></Modal></Screen>;
}

function DateCard({ date, label, onPress }: { date: Date; label: string; onPress: () => void }) { return <View style={styles.dateGroup}><Text style={styles.label}>{label}</Text><Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.dateCard, pressed && styles.pressed]}><Text style={styles.day}>{formatDay(date)}</Text><Text style={styles.cardTime}>{toTime(date)}</Text></Pressable></View>; }
function PickerRow({ label, value, onMinus, onPlus }: { label: string; value: string; onMinus: () => void; onPlus: () => void }) { return <View style={styles.pickerRow}><Text style={styles.pickerLabel}>{label}</Text><View style={styles.picker}><Pressable accessibilityRole="button" onPress={onMinus} style={styles.timeButton}><Text style={styles.timeButtonText}>−</Text></Pressable><Text style={styles.time}>{value}</Text><Pressable accessibilityRole="button" onPress={onPlus} style={styles.timeButton}><Text style={styles.timeButtonText}>+</Text></Pressable></View></View>; }

const styles = StyleSheet.create({
  screen: { flex: 1 }, content: { flex: 1, paddingTop: 27 }, heading: { ...typography.title, fontSize: 29, lineHeight: 33 }, label: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 14, marginTop: 24 }, input: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 16, borderWidth: 3, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 17, marginTop: 9, minHeight: 56, paddingHorizontal: 15 }, dateRow: { flexDirection: "row", gap: 12, marginTop: 21 }, dateGroup: { flex: 1 }, dateCard: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 16, borderWidth: 3, marginTop: 9, minHeight: 78, paddingHorizontal: 15, paddingVertical: 10 }, day: { color: colors.surfaceRaised, fontFamily: "ZenMaruBold", fontSize: 12 }, cardTime: { color: colors.ink, fontFamily: "Bangers", fontSize: 29, marginTop: 1, paddingRight: 3 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 14, textAlign: "center" }, overlay: { alignItems: "center", flex: 1, justifyContent: "center", paddingHorizontal: 28 }, backdrop: { backgroundColor: "rgba(8, 2, 15, 0.78)", bottom: 0, left: 0, position: "absolute", right: 0, top: 0 }, modal: { backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 22, borderWidth: 3, padding: 22, width: "100%" }, modalTitle: { color: colors.text, fontFamily: "Bangers", fontSize: 27, paddingRight: 3, textAlign: "center" }, modalSubtitle: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 13, marginTop: 5, textAlign: "center" }, pickerRow: { marginTop: 19 }, pickerLabel: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 12, textAlign: "center" }, picker: { alignItems: "center", flexDirection: "row", justifyContent: "space-between", marginTop: 7 }, timeButton: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 14, borderWidth: 2, height: 44, justifyContent: "center", width: 44 }, timeButtonText: { color: colors.lime, fontFamily: "ZenMaruBold", fontSize: 25 }, time: { color: colors.text, fontFamily: "Bangers", fontSize: 31, paddingRight: 3, textTransform: "uppercase" }, doneButton: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 14, borderWidth: 2, marginTop: 24, paddingVertical: 12 }, doneText: { color: colors.ink, fontFamily: "Bangers", fontSize: 20, paddingRight: 3 }, pressed: { opacity: 0.82, transform: [{ scale: 0.98 }] },
});
