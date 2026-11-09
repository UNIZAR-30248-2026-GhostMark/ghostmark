import * as SecureStore from "expo-secure-store";

// «Te han cazado» se enseña una vez por partida (R11-25): el móvil recuerda que ya se vio.
function key(gameId: string) {
  return `cazado-visto-${gameId.replace(/[^A-Za-z0-9._-]/g, "_")}`;
}

export async function hasSeenCaught(gameId: string) {
  try {
    return (await SecureStore.getItemAsync(key(gameId))) === "1";
  } catch {
    return false;
  }
}

export async function markCaughtSeen(gameId: string) {
  try {
    await SecureStore.setItemAsync(key(gameId), "1");
  } catch {
    // Si no se puede guardar, la pantalla se vuelve a ver la próxima vez: no se pierde nada.
  }
}
