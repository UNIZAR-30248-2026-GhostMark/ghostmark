import type { Schedule } from "./schedule";

export type Inscription = {
  id: string;
  partidaId: string;
  inscritaEn: string;
  horario: Schedule;
};

export type Player = {
  id: string;
  alias: string | null;
  esMaster: boolean;
  // El servidor de eliminación añadirá este dato; mientras tanto, un inscrito en
  // una partida en juego se representa como vivo.
  estado?: "VIVO" | "ELIMINADO" | "ABANDONO" | "EXPULSADO";
};
