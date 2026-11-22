import type { Dispute } from "../types/game";

const MONTHS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"];

export type Bubble = { align: "left" | "right"; author: string; text: string };

export function aliasOrDeleted(alias: string | null) {
  return alias ?? "Cuenta eliminada";
}

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// Fecha de apertura bajo la misión (R12-34): «hoy, 18:02», «ayer, 9:05» o «12 nov, 18:02».
export function disputeDate(iso: string, now = new Date()) {
  const date = new Date(iso);
  const time = `${date.getHours()}:${String(date.getMinutes()).padStart(2, "0")}`;
  if (sameDay(date, now)) return `hoy, ${time}`;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(date, yesterday)) return `ayer, ${time}`;
  return `${date.getDate()} ${MONTHS[date.getMonth()]}, ${time}`;
}

export function disputeHeader(dispute: Dispute, now = new Date()) {
  const hunter = aliasOrDeleted(dispute.aliasCazador);
  const victim = aliasOrDeleted(dispute.aliasVictima);
  const what = dispute.tipo === "NEGATIVA"
    ? `${hunter} dice que ${victim} no le enseña el código`
    : `${hunter} eliminó a ${victim}`;
  return `${what} · ${disputeDate(dispute.abiertaEn, now)}`;
}

// La víctima habla a la izquierda y el cazador a la derecha (UI19). En una eliminación abre la
// víctima y responde el cazador; en una negativa, al revés. Se enseñan en el orden en que se escribieron.
export function disputeBubbles(dispute: Dispute): Bubble[] {
  const victim: Omit<Bubble, "text"> = { align: "left", author: `${aliasOrDeleted(dispute.aliasVictima)} · víctima` };
  const hunter: Omit<Bubble, "text"> = { align: "right", author: `${aliasOrDeleted(dispute.aliasCazador)} · cazador` };
  const [opens, answers] = dispute.tipo === "NEGATIVA" ? [hunter, victim] : [victim, hunter];
  const bubbles: Bubble[] = [{ ...opens, text: dispute.textoApertura }];
  if (dispute.textoRespuesta) bubbles.push({ ...answers, text: dispute.textoRespuesta });
  return bubbles;
}

// Botones del árbitro (modelo de datos, «Decidir»): dar la razón al cazador o a la víctima.
export function decisionLabels(dispute: Dispute) {
  return dispute.tipo === "NEGATIVA"
    ? { hunter: `ELIMINAR A ${aliasOrDeleted(dispute.aliasVictima).toUpperCase()}`, victim: "RECHAZAR" }
    : { hunter: "CONFIRMAR ELIMINACIÓN", victim: "ANULAR" };
}

export function disputeWaiting(dispute: Dispute) {
  if (dispute.estado !== "ABIERTA" || dispute.soyArbitro || dispute.puedoResponder) return null;
  return dispute.textoRespuesta ? "Esperando la decisión del árbitro." : "Esperando la respuesta de la otra persona.";
}

// Resultado al final de la disputa (R12-35).
export function disputeOutcome(dispute: Dispute) {
  if (dispute.estado === "SIN_OBJETO") {
    return {
      title: dispute.tipo === "ELIMINACION"
        ? "DISPUTA CERRADA: ELIMINACIÓN ANULADA"
        : "DISPUTA CERRADA: YA NO ES SU OBJETIVO",
      detail: null,
    };
  }
  if (dispute.estado !== "RESUELTA") return null;
  return {
    title: dispute.razon === "CAZADOR" ? "GANA EL CAZADOR" : "GANA LA VÍCTIMA",
    detail: dispute.resueltaPor === "ZONA" ? "Estaba en una zona segura" : null,
  };
}

export function disputeListMeta(dispute: Dispute) {
  if (dispute.estado === "ABIERTA") return "ABIERTA · VER Y ACTUAR";
  return dispute.estado === "SIN_OBJETO" ? "CERRADA · VER DETALLE" : "RESUELTA · VER DETALLE";
}
