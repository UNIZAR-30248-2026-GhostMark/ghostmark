export function parseInvitationLink(url: string): string | undefined {
  // Expo Go usa /--/ para separar la URL del proyecto de la ruta de la app.
  const match = /^(?:ghostmark:\/\/unirse\/|exps?:\/\/[^/]+\/--\/unirse\/)([23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8})\/?$/i.exec(url);
  return match?.[1]?.toUpperCase();
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
