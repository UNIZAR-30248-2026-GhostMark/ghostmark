export type MissionType = "BIBLIOTECA" | "IA";

export type GameConfiguration = {
  nombre: string;
  inicioEn: string;
  finEn: string;
  tipoMisiones: MissionType;
  plazoDisputaHoras: 2 | 6 | 12 | 24;
  entradasTardias: boolean;
  masterJuega: boolean;
};

export type Game = GameConfiguration & {
  id: string;
  estado: "EN_INSCRIPCION" | "EN_JUEGO" | "CANCELADA" | "CERRADA";
  codigoInvitacion: string;
  enlace: string;
  numInscritos: number;
  creadaEn: string;
};

export type GameSummary = Pick<Game, "id" | "nombre" | "estado" | "inicioEn" | "finEn" | "numInscritos"> & { soyMaster: boolean };
export type GameDraft = Pick<GameConfiguration, "nombre" | "inicioEn" | "finEn">;

export type PublicGame = Pick<Game, "nombre" | "estado" | "inicioEn" | "finEn" | "numInscritos"> & {
  aliasMaster: string | null;
};
