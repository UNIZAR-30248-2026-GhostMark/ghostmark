import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as Location from "expo-location";
import { useCallback, useState } from "react";
import { Alert, Image, Linking, Pressable, StyleSheet, Text, View } from "react-native";

import { deleteAccount } from "../api/account";
import { clearSession, getCurrentAccount, signOut as closeSession } from "../api/auth";
import { Screen } from "../components/Screen";
import { colors } from "../theme/colors";
import type { RootStackParamList } from "../types/navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Profile">;

export function ProfileScreen({ navigation }: Props) {
  const account = getCurrentAccount();
  const [error, setError] = useState<string>();
  const [isDeleting, setIsDeleting] = useState(false);
  const [locationGranted, setLocationGranted] = useState(false);

  const refreshLocationPermission = useCallback(async () => {
    try {
      const permission = await Location.getForegroundPermissionsAsync();
      setLocationGranted(permission.granted);
    } catch {
      setLocationGranted(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void refreshLocationPermission();
  }, [refreshLocationPermission]));

  function removeAccount() {
    Alert.alert(
      "¿Borrar tu cuenta?",
      "Se eliminarán tu alias, datos de acceso, horario y amistades. Esta acción no se puede deshacer.",
      [
        { style: "cancel", text: "Cancelar" },
        { style: "destructive", text: "Borrar cuenta", onPress: confirmDelete },
      ],
    );
  }

  async function confirmDelete() {
    setError(undefined);
    setIsDeleting(true);
    try {
      await deleteAccount();
      await clearSession();
      navigation.reset({ index: 0, routes: [{ name: "Welcome" }] });
    } catch {
      setError("No hemos podido borrar la cuenta. Comprueba la conexión e inténtalo de nuevo.");
    } finally {
      setIsDeleting(false);
    }
  }

  async function signOut() {
    try {
      await closeSession();
      navigation.reset({ index: 0, routes: [{ name: "Welcome" }] });
    } catch {
      setError("No hemos podido cerrar la sesión. Comprueba la conexión e inténtalo de nuevo.");
    }
  }

  async function updateLocationPermission() {
    setError(undefined);
    try {
      const currentPermission = await Location.getForegroundPermissionsAsync();
      if (currentPermission.granted) {
        setLocationGranted(true);
        return;
      }
      if (!currentPermission.canAskAgain) {
        await Linking.openSettings();
        return;
      }
      const requestedPermission = await Location.requestForegroundPermissionsAsync();
      setLocationGranted(requestedPermission.granted);
      if (!requestedPermission.granted && !requestedPermission.canAskAgain) {
        await Linking.openSettings();
      }
    } catch {
      setError("No hemos podido consultar el permiso de ubicación. Inténtalo de nuevo.");
    }
  }

  return (
    <Screen padded={false}>
      <View style={styles.screen}>
        <View style={styles.content}>
          <View style={styles.profileCard}>
            <Image accessibilityLabel="Fantasma de perfil" source={require("../../assets/ghosts/fantasma-saluda.png")} style={styles.profileGhost} />
            <View style={styles.profileInfo}><Text style={styles.profileName}>{account?.alias ?? "TU CUENTA"}</Text><Text style={styles.profileMethod}>Perfil de GhostMark</Text></View>
          </View>

          <View style={styles.menu}>
            <MenuItem icon="calendar-outline" label="Mi horario lectivo" onPress={() => navigation.navigate("Schedule")} />
            <Pressable accessibilityLabel="Actualizar permiso de ubicación" accessibilityRole="button" onPress={updateLocationPermission} style={({ pressed }) => [styles.menuItem, pressed && styles.pressed]}>
              <View style={styles.menuLeft}><Ionicons color={colors.textMuted} name="location-outline" size={19} /><Text style={styles.menuText}>Permisos</Text></View>
              <View style={[styles.locationBadge, !locationGranted && styles.locationBadgeInactive]}><Text style={[styles.locationBadgeText, !locationGranted && styles.locationBadgeTextInactive]}>{locationGranted ? "Ubicación activa" : "Ubicación desactivada"}</Text></View>
            </Pressable>
            <MenuItem icon="log-out-outline" label="Cerrar sesión" onPress={signOut} />
          </View>

          <View style={styles.deleteSection}>
            <Pressable accessibilityRole="button" disabled={isDeleting} onPress={removeAccount} style={({ pressed }) => [styles.deleteButton, pressed && !isDeleting && styles.pressed, isDeleting && styles.disabled]}>
              <Ionicons color={colors.white} name="trash-outline" size={22} /><Text style={styles.deleteButtonText}>{isDeleting ? "BORRANDO…" : "BORRAR MI CUENTA"}</Text>
            </Pressable>
            <Text style={styles.deleteDescription}>Se borran tu cuenta y todos tus datos. Te pediremos confirmación.</Text>
          </View>
          {error ? <Text accessibilityLiveRegion="polite" style={styles.error}>{error}</Text> : null}
        </View>

        <View style={styles.bottomBar}>
          <Pressable accessibilityRole="button" onPress={() => navigation.navigate("Home")} style={({ pressed }) => [styles.navItem, pressed && styles.pressed]}><Ionicons color={colors.textMuted} name="game-controller-outline" size={19} /><Text style={styles.navText}>Partidas</Text></Pressable>
          <View style={styles.navItem}><Ionicons color={colors.textMuted} name="people-outline" size={19} /><Text style={styles.navText}>Amigos</Text></View>
          <View style={[styles.navItem, styles.navItemActive]}><Ionicons color={colors.ink} name="person-circle-outline" size={20} /><Text style={[styles.navText, styles.navTextActive]}>Perfil</Text></View>
        </View>
      </View>
    </Screen>
  );
}

function MenuItem({ icon, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress: () => void }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.menuItem, pressed && styles.pressed]}><View style={styles.menuLeft}><Ionicons color={colors.textMuted} name={icon} size={19} /><Text style={styles.menuText}>{label}</Text></View><Ionicons color={colors.textMuted} name="chevron-forward" size={20} /></Pressable>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background, flex: 1 }, content: { flex: 1, paddingHorizontal: 24, paddingTop: 18 },
  profileCard: { alignItems: "center", backgroundColor: "#D8C4FF", borderColor: colors.ink, borderRadius: 20, borderWidth: 2, flexDirection: "row", minHeight: 154, overflow: "hidden", paddingHorizontal: 16, shadowColor: colors.border, shadowOffset: { width: 0, height: 5 }, shadowOpacity: 1, shadowRadius: 0 }, profileGhost: { height: 145, resizeMode: "contain", width: 145 }, profileInfo: { flex: 1, marginLeft: 4 }, profileName: { color: colors.ink, fontFamily: "Bangers", fontSize: 25, letterSpacing: 0.7 }, profileMethod: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 13, marginTop: 5 },
  menu: { gap: 10, marginTop: 34 }, menuItem: { alignItems: "center", backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderRadius: 10, borderWidth: 3, flexDirection: "row", justifyContent: "space-between", minHeight: 62, paddingHorizontal: 14 }, menuLeft: { alignItems: "center", flexDirection: "row", gap: 12 }, menuText: { color: colors.text, fontFamily: "ZenMaruBold", fontSize: 15 }, locationBadge: { backgroundColor: colors.lime, borderColor: colors.ink, borderRadius: 8, borderWidth: 1, paddingHorizontal: 10, paddingVertical: 5 }, locationBadgeInactive: { backgroundColor: colors.danger }, locationBadgeText: { color: colors.ink, fontFamily: "ZenMaruBold", fontSize: 10 }, locationBadgeTextInactive: { color: colors.white },
  deleteSection: { marginTop: 26 }, deleteButton: { alignItems: "center", backgroundColor: colors.danger, borderColor: colors.ink, borderRadius: 12, borderWidth: 2, flexDirection: "row", gap: 10, justifyContent: "center", minHeight: 55, shadowColor: colors.border, shadowOffset: { width: 0, height: 5 }, shadowOpacity: 1, shadowRadius: 0 }, deleteButtonText: { color: colors.white, fontFamily: "Bangers", fontSize: 22, letterSpacing: 0.8 }, deleteDescription: { color: colors.textMuted, fontFamily: "ZenMaru", fontSize: 12, lineHeight: 18, marginTop: 13, paddingHorizontal: 4 }, error: { color: colors.danger, fontFamily: "ZenMaruBold", fontSize: 12, lineHeight: 16, marginTop: 14, textAlign: "center" }, pressed: { opacity: 0.8, transform: [{ scale: 0.98 }] }, disabled: { opacity: 0.65 },
  bottomBar: { backgroundColor: colors.surface, flexDirection: "row", paddingBottom: 10, paddingHorizontal: 10, paddingTop: 9 }, navItem: { alignItems: "center", borderRadius: 14, flex: 1, flexDirection: "row", gap: 7, justifyContent: "center", minHeight: 46 }, navItemActive: { backgroundColor: colors.lime }, navText: { color: colors.textMuted, fontFamily: "ZenMaruBold", fontSize: 13 }, navTextActive: { color: colors.ink },
});
