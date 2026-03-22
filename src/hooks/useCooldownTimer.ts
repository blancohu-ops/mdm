import { useEffect, useState } from "react";

export function useCooldownTimer() {
  const [cooldownSeconds, setCooldownSeconds] = useState(0);

  useEffect(() => {
    if (cooldownSeconds <= 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setCooldownSeconds((current) => Math.max(current - 1, 0));
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [cooldownSeconds]);

  return {
    cooldownSeconds,
    coolingDown: cooldownSeconds > 0,
    startCooldown: (seconds: number) => setCooldownSeconds(Math.max(0, Math.floor(seconds))),
    resetCooldown: () => setCooldownSeconds(0),
  };
}
