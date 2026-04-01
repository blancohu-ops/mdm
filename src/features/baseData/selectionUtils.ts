import type { DictItem } from "@/types/dictionary";

export function splitDelimitedNames(value: string) {
  return Array.from(
    new Set(
      value
        .split(/[,\n\r;、，；]+/)
        .map((item) => item.trim())
        .filter(Boolean),
    ),
  );
}

export function joinDelimitedNames(values: string[]) {
  return values
    .map((item) => item.trim())
    .filter(Boolean)
    .join(",");
}

export function toggleSelection(
  values: string[],
  value: string,
  checked: boolean,
  limit?: number,
) {
  const nextValues = checked
    ? [...values, value]
    : values.filter((item) => item !== value);
  const uniqueValues = Array.from(
    new Set(nextValues.map((item) => item.trim()).filter(Boolean)),
  );

  if (typeof limit === "number") {
    return uniqueValues.slice(0, limit);
  }

  return uniqueValues;
}

export function normalizeDictName(value: string, options: DictItem[]) {
  const normalizedValue = value.trim();
  if (!normalizedValue) {
    return "";
  }

  const matchedOption = options.find(
    (item) => item.name === normalizedValue || item.code === normalizedValue,
  );
  return matchedOption?.name ?? normalizedValue;
}

export function normalizeDictNames(values: string[], options: DictItem[]) {
  return Array.from(
    new Set(
      values
        .map((value) => normalizeDictName(value, options))
        .filter(Boolean),
    ),
  );
}
