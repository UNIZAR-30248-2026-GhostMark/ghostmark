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
  // Con la partida en juego o cerrada; nulo (o sin llegar) en inscripción. Lo manda la 8.2.
  estado?: "VIVO" | "ELIMINADO" | null;
};
