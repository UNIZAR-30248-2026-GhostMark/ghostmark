const invitationCode = /^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8}$/;

export function invitationLink(code: string) {
  if (!invitationCode.test(code)) throw new Error("Código de invitación no válido");
  return `ghostmark://unirse/${code}`;
}

export function parseInvitationLink(url: string): string | undefined {
  // Expo Go usa /--/ para separar la URL del proyecto de la ruta de la app.
  const match = /^(?:ghostmark:\/\/unirse\/|exps?:\/\/[^/]+\/--\/unirse\/)([23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8})\/?$/.exec(url);
  return match?.[1];
}

export function createInvitationInbox() {
  let pending: string | undefined;
  return {
    receive(url: string) {
      const code = parseInvitationLink(url);
      if (code) pending = code;
    },
    take(ready: boolean): string | undefined {
      if (!ready) return undefined;
      const code = pending;
      pending = undefined;
      return code;
    },
  };
}
