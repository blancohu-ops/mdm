export async function mockDelay<T>(data: T, ms = 180): Promise<T> {
  await new Promise((resolve) => window.setTimeout(resolve, ms));
  return JSON.parse(JSON.stringify(data)) as T;
}
