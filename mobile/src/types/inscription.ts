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
};
