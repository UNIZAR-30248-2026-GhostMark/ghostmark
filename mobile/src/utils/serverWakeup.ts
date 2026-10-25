import { useEffect, useState } from "react";

export const serverWakeupDelayMs = 5_000;
export const serverWakeupMessage = "Despertando el servidor; la primera vez puede tardar hasta un minuto.";

export function useServerWakeupNotice(pending: boolean, requestId = 0) {
  const [noticeRequestId, setNoticeRequestId] = useState<number>();

  useEffect(() => {
    if (!pending) return undefined;
    const timer = setTimeout(() => setNoticeRequestId(requestId), serverWakeupDelayMs);
    return () => clearTimeout(timer);
  }, [pending, requestId]);

  return pending && noticeRequestId === requestId;
}
