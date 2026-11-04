/** Erro devolvido pela API (ou falha de comunicação). `body` segue o padrão da spec 8.3. */
export class ApiError extends Error {
  constructor(body, status) {
    super(body?.message ?? 'Erro inesperado.');
    this.body = body ?? {};
    this.status = status;
  }
}

export async function api(method, path, body) {
  let response;
  try {
    response = await fetch(path, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      { message: 'Não foi possível se comunicar com o sistema. Verifique se ele está em execução.' }, 0);
  }

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new ApiError(data ?? { message: 'Erro inesperado ao processar a requisição.' }, response.status);
  }
  return data;
}