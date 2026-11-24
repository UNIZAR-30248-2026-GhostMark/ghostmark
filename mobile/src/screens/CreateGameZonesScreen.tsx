import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { ApiError } from "../api/client";
import { createGame, getSafeZones, saveSafeZones } from "../api/game";
import { MapZones } from "../components/MapZones";
import { GameWizardHeader } from "../components/GameWizardHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { Screen } from "../components/Screen";
import { bangers } from "../theme/bangers";
import { colors } from "../theme/colors";
import type { SafeZone, ZonePoint } from "../types/game";
import type { RootStackParamList } from "../types/navigation";
import { hasCrossedSides, overlappingZone } from "../utils/safeZoneGeometry";

type Props = NativeStackScreenProps<RootStackParamList, "CreateGameZones">;

const MAX_ZONES = 10;
const MAX_POINTS = 20;

export function CreateGameZonesScreen({ navigation, route }: Props) {
  const [zones, setZones] = useState<SafeZone[]>([]);
  const [draftPoints, setDraftPoints] = useState<ZonePoint[]>([]);
  const [editing, setEditing] = useState<SafeZone>();
  const [name, setName] = useState("");
  const [nameOpen, setNameOpen] = useState(false);
  const [deleteCandidate, setDeleteCandidate] = useState<SafeZone>();
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  // Si el PUT falla después del POST, reintentar guarda sobre la misma partida.
  const [createdGame, setCreatedGame] = useState("configuration" in route.params ? route.params.game : undefined);
  // Desde la sala de espera (R12-32) se editan las zonas de una partida ya creada.
  const editGameId = "editGameId" in route.params ? route.params.editGameId : undefined;
  const [loading, setLoading] = useState(Boolean(editGameId));

  useEffect(() => {
    if (!editGameId) return undefined;
    let active = true;
    getSafeZones(editGameId).then((result) => { if (active) setZones(result.zonas); })
      .catch(() => { if (active) setError("No hemos podido cargar las zonas. Vuelve atrás e inténtalo de nuevo."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [editGameId]);

  function addPoint(point: ZonePoint) {
    // El servidor rechaza los puntos repetidos y más de 20 por zona: se avisa antes de guardar.
    if (draftPoints.some((item) => item.lat === point.lat && item.lon === point.lon)) { setError("Ese punto ya está marcado."); return; }
    if (draftPoints.length >= MAX_POINTS) { setError(`Una zona tiene como máximo ${MAX_POINTS} puntos.`); return; }
    if (!editing && draftPoints.length === 0 && zones.length >= MAX_ZONES) { setError(`Como máximo hay ${MAX_ZONES} zonas por partida.`); return; }
    setError(undefined);
    setDraftPoints((points) => [...points, point]);
  }

  function undoPoint() {
    // Si se deshacen todos los puntos de una zona que se editaba, vuelve tal como estaba.
    if (editing && draftPoints.length === 1) {
      setZones((items) => [...items, editing]);
      setEditing(undefined);
    }
    setDraftPoints((points) => points.slice(0, -1));
  }

  function openName() {
    if (draftPoints.length < 3) { setError("Marca al menos 3 puntos para crear una zona."); return; }
    if (hasCrossedSides(draftPoints)) { setError("Los lados de esta zona se cruzan. Corrige los puntos antes de terminar."); return; }
    const overlap = overlappingZone(draftPoints, zones);
    if (overlap) { setError(`Esta zona se solapa con «${overlap.nombre}». Cambia sus puntos antes de terminar.`); return; }
    setName(editing?.nombre ?? "");
    setNameOpen(true);
  }

  function finishZone() {
    const trimmed = name.trim();
    if (!trimmed) { setError("Escribe un nombre para la zona."); return; }
    if (trimmed.length > 30) { setError("El nombre puede tener como máximo 30 caracteres."); return; }
    if (zones.some((zone) => zone.nombre.localeCompare(trimmed, "es", { sensitivity: "accent" }) === 0)) { setError("Ya hay una zona con ese nombre."); return; }
    setZones((items) => [...items, { id: editing?.id, nombre: trimmed, puntos: draftPoints }]);
    setDraftPoints([]); setEditing(undefined); setNameOpen(false); setError(undefined);
  }

  function edit(zone: SafeZone) {
    setZones((items) => items.filter((item) => item !== zone));
    setEditing(zone); setDraftPoints(zone.puntos); setError(undefined);
  }

  function remove(zone: SafeZone) {
    setDeleteCandidate(zone);
  }

  async function saveEdited(gameId: string) {
    setSaving(true); setError(undefined);
    try {
      await saveSafeZones(gameId, zones);
      navigation.goBack();
    } catch (requestError) {
      setError(requestError instanceof ApiError && requestError.code === "PARTIDA_NO_EDITABLE"
        ? "La partida ya ha arrancado: las zonas no se pueden cambiar."
        : requestError instanceof ApiError ? requestError.message : "No hemos podido guardar las zonas. Comprueba la conexión e inténtalo de nuevo.");
    } finally { setSaving(false); }
  }

  async function continueToShare() {
    if (!("configuration" in route.params)) return;
    const { configuration } = route.params;
    setSaving(true); setError(undefined);
    try {
      const game = createdGame ?? await createGame(configuration);
      setCreatedGame(game);
      await saveSafeZones(game.id, zones);
      navigation.reset({ index: 1, routes: [homeRoute(game), { name: "ShareGame", params: { game } }] });
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "No hemos podido guardar las zonas. Comprueba la conexión e inténtalo de nuevo.");
    } finally { setSaving(false); }
  }

  return <Screen><View style={styles.screen}>
    {editGameId
      ? <View style={styles.editHeader}><Pressable accessibilityLabel="Volver a la sala de espera" accessibilityRole="button" onPress={() => navigation.goBack()} style={styles.editBack}><Text style={styles.editBackText}>‹</Text></Pressable><Text style={styles.editTitle}>ZONAS SEGURAS</Text></View>
      : <GameWizardHeader label="Zonas seguras" onBack={() => navigation.goBack()} step={4} />}
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <MapZones draftPoints={draftPoints} editable onAddPoint={addPoint} zones={zones} />
      <View style={styles.mapActions}>
        <Pressable accessibilityLabel="Deshacer último punto" accessibilityRole="button" disabled={draftPoints.length === 0} onPress={undoPoint} style={[styles.undo, draftPoints.length === 0 && styles.dimmed]}><Text adjustsFontSizeToFit minimumFontScale={0.68} numberOfLines={1} style={styles.undoText}>DESHACER PUNTO</Text></Pressable>
        <Pressable accessibilityLabel="Terminar zona" accessibilityRole="button" disabled={draftPoints.length < 3} onPress={openName} style={[styles.finish, draftPoints.length < 3 && styles.dimmed]}><Text adjustsFontSizeToFit minimumFontScale={0.68} numberOfLines={1} style={styles.finishText}>TERMINAR</Text></Pressable>
      </View>
      <Text style={styles.help}>{draftPoints.length ? `${draftPoints.length} puntos marcados${editing ? " · editando zona" : ""}` : "Toca el mapa para marcar los vértices de una zona."}</Text>
      <View style={styles.list}>{zones.map((zone) => <View key={`${zone.id ?? "nueva"}-${zone.nombre}`} style={styles.zoneRow}><View style={styles.zoneInfo}><Text style={styles.shield}>⌾</Text><View><Text style={styles.zoneName}>{zone.nombre}</Text><Text style={styles.zonePoints}>{zone.puntos.length} puntos</Text></View></View><View style={styles.rowActions}><Pressable accessibilityLabel={`Editar ${zone.nombre}`} accessibilityRole="button" onPress={() => edit(zone)} style={styles.smallButton}><Text adjustsFontSizeToFit minimumFontScale={0.75} numberOfLines={1} style={styles.smallText}>EDITAR</Text></Pressable><Pressable accessibilityLabel={`Borrar ${zone.nombre}`} accessibilityRole="button" onPress={() => remove(zone)} style={[styles.smallButton, styles.deleteButton]}><Text adjustsFontSizeToFit minimumFontScale={0.75} numberOfLines={1} style={styles.smallText}>ELIMINAR</Text></Pressable></View></View>)}</View>
      {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
    </ScrollView>
    <PrimaryButton disabled={saving || loading || draftPoints.length > 0} onPress={() => void (editGameId ? saveEdited(editGameId) : continueToShare())}>{saving ? "GUARDANDO…" : editGameId ? "GUARDAR" : "SIGUIENTE"}</PrimaryButton>
    <Modal animationType="fade" onRequestClose={() => setNameOpen(false)} transparent visible={nameOpen}><View style={styles.overlay}><View style={styles.modal}><Text style={styles.modalTitle}>NOMBRA LA ZONA</Text><TextInput accessibilityLabel="Nombre de la zona" autoFocus maxLength={30} onChangeText={setName} placeholder="Biblioteca" placeholderTextColor={colors.textMuted} style={styles.input} value={name} /><View style={styles.modalActions}><Pressable accessibilityRole="button" onPress={() => setNameOpen(false)} style={styles.cancel}><Text style={styles.cancelText}>CANCELAR</Text></Pressable><Pressable accessibilityRole="button" onPress={finishZone} style={styles.confirm}><Text style={styles.confirmText}>GUARDAR</Text></Pressable></View></View></View></Modal>
    <Modal animationType="fade" onRequestClose={() => setDeleteCandidate(undefined)} transparent visible={Boolean(deleteCandidate)}><View style={styles.overlay}><View style={styles.modal}><Text style={styles.modalTitle}>¿BORRAR ZONA?</Text><Text style={styles.deleteDescription}>Se borrará «{deleteCandidate?.nombre}».</Text><View style={styles.modalActions}><Pressable accessibilityRole="button" onPress={() => setDeleteCandidate(undefined)} style={styles.cancel}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.cancelText}>CANCELAR</Text></Pressable><Pressable accessibilityRole="button" onPress={() => { if (deleteCandidate) setZones((items) => items.filter((item) => item !== deleteCandidate)); setDeleteCandidate(undefined); }} style={styles.deleteConfirm}><Text adjustsFontSizeToFit minimumFontScale={0.7} numberOfLines={1} style={styles.confirmText}>BORRAR</Text></Pressable></View></View></View></Modal>
  </View></Screen>;
}

function homeRoute(game: import("../types/game").Game) {
  return { name: "Home" as const, params: { createdGame: { finEn: game.finEn, id: game.id, inicioEn: game.inicioEn, nombre: game.nombre, estado: game.estado, motivoCancelacion: null, numInscritos: game.numInscritos, resultado: game.resultado, soyJugador: game.masterJuega, soyMaster: true } } };
}

const styles = StyleSheet.create({
  screen: { flex: 1 }, content: { flexGrow: 1, paddingTop: 13 },
  editHeader: { alignItems: "center", flexDirection: "row", gap: 12, paddingTop: 6 }, editBack: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 12, borderWidth: 2, height: 42, justifyContent: "center", width: 42 }, editBackText: { color: colors.text, fontSize: 26, lineHeight: 28 }, editTitle: { color: colors.text, flex: 1, ...bangers(32) },
  mapActions: { flexDirection: "row", gap: 10, marginTop: -49, paddingHorizontal: 13, zIndex: 2 },
  undo: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 10, borderWidth: 3, boxShadow: `0px 4px 0px ${colors.ink}`, flex: 1.2, justifyContent: "center", minHeight: 43, paddingHorizontal: 7, paddingVertical: 3 },
  finish: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 10, borderWidth: 3, boxShadow: `0px 4px 0px ${colors.ink}`, flex: 1, justifyContent: "center", minHeight: 43, paddingHorizontal: 7, paddingVertical: 3 },
  undoText: { alignSelf: "stretch", color: colors.text, ...bangers(16), lineHeight: 20, textAlign: "center" }, finishText: { alignSelf: "stretch", color: colors.ink, ...bangers(16), lineHeight: 20, textAlign: "center" }, dimmed: { opacity: 0.5 },
  help: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 12, lineHeight: 17, marginTop: 15, textAlign: "center" }, list: { gap: 9, marginTop: 16 },
  zoneRow: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 11, borderWidth: 3, flexDirection: "row", justifyContent: "space-between", minHeight: 59, paddingHorizontal: 12 }, zoneInfo: { alignItems: "center", flex: 1, flexDirection: "row", gap: 10 }, shield: { color: colors.lime, fontSize: 25 }, zoneName: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 14 }, zonePoints: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 11, marginTop: 1 }, rowActions: { flexDirection: "row", gap: 6 }, smallButton: { alignItems: "center", borderColor: colors.textMuted, borderRadius: 7, borderWidth: 1, justifyContent: "center", minHeight: 39, paddingHorizontal: 8, paddingVertical: 4 }, deleteButton: { borderColor: colors.danger, minWidth: 65 }, smallText: { alignSelf: "stretch", color: colors.text, ...bangers(12), lineHeight: 18, textAlign: "center" },
  error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 13, lineHeight: 18, marginTop: 14, textAlign: "center" },
  overlay: { alignItems: "center", backgroundColor: "rgba(8,2,15,.75)", flex: 1, justifyContent: "center", paddingHorizontal: 28 }, modal: { backgroundColor: colors.surface, borderColor: colors.border, borderRadius: 19, borderWidth: 3, padding: 21, width: "100%" }, modalTitle: { color: colors.text, ...bangers(25), textAlign: "center" }, deleteDescription: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 16, lineHeight: 23, marginTop: 14, textAlign: "center" }, input: { backgroundColor: colors.white, borderColor: colors.ink, borderRadius: 11, borderWidth: 2, color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 15, marginTop: 18, minHeight: 48, paddingHorizontal: 12 }, modalActions: { flexDirection: "row", gap: 10, marginTop: 17 }, cancel: { alignItems: "center", borderColor: colors.textMuted, borderRadius: 10, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 48, paddingHorizontal: 7, paddingVertical: 4 }, confirm: { alignItems: "center", backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 48, paddingHorizontal: 7, paddingVertical: 4 }, deleteConfirm: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 10, borderWidth: 2, flex: 1, justifyContent: "center", minHeight: 48, paddingHorizontal: 7, paddingVertical: 4 }, cancelText: { alignSelf: "stretch", color: colors.text, ...bangers(15), lineHeight: 21, textAlign: "center" }, confirmText: { alignSelf: "stretch", color: colors.ink, ...bangers(15), lineHeight: 21, textAlign: "center" },
});
