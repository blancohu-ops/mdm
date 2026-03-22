import { useCallback } from "react";
import { unstable_usePrompt as usePrompt, useBeforeUnload } from "react-router-dom";

const DEFAULT_MESSAGE = "当前页面有未保存的修改，确认离开吗？";

export function useUnsavedChangesGuard(enabled: boolean, message = DEFAULT_MESSAGE) {
  usePrompt({ when: enabled, message });

  const handleBeforeUnload = useCallback(
    (event: BeforeUnloadEvent) => {
      if (!enabled) {
        return;
      }

      event.preventDefault();
      event.returnValue = message;
    },
    [enabled, message],
  );

  useBeforeUnload(handleBeforeUnload, { capture: true });
}
