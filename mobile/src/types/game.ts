export type MissionType = "BIBLIOTECA" | "IA";
export type GameResultType = "SUPERVIVIENTE" | "GANADOR" | "EMPATE";
export type GameCloseReason = "ULTIMO_SUPERVIVIENTE" | "FECHA_FIN" | "MASTER";

export type GameResult = {
  tipo: GameResultType;
  motivo: GameCloseReason;
  aliasGanadores: (string | null)[];
  eliminaciones: number;
  cerradaEn: string;
};

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
  resultado: GameResult | null;
};

export type CancellationReason = "POCOS_JUGADORES" | "MASTER_SIN_CUENTA";

export type GameSummary = Pick<Game, "id" | "nombre" | "estado" | "inicioEn" | "finEn" | "numInscritos"> & {
  soyMaster: boolean;
  soyJugador: boolean;
  motivoCancelacion: CancellationReason | null;
  resultado: GameResult | null;
};
export type GameDraft = Pick<GameConfiguration, "nombre" | "inicioEn" | "finEn">;

export type PublicGame = Pick<Game, "nombre" | "estado" | "inicioEn" | "finEn" | "numInscritos"> & {
  aliasMaster: string | null;
};
