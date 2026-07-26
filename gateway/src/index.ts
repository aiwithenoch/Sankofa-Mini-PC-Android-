interface Env {
  COMPOSIO_API_KEY: string;
  SANKOFA_GATEWAY_TOKEN: string;
  COMPOSIO_BASE_URL?: string;
  ALLOWED_ORIGIN?: string;
  ALLOWED_TOOL_PREFIXES?: string;
}

type ToolRisk = "READ_ONLY" | "DRAFT" | "EXTERNAL_WRITE" | "DESTRUCTIVE";

type JsonRecord = Record<string, unknown>;

const MAX_BODY_BYTES = 64 * 1024;
const DEFAULT_COMPOSIO_BASE = "https://backend.composio.dev";
const DEFAULT_PREFIXES = [
  "GMAIL_",
  "GOOGLECALENDAR_",
  "GOOGLEDRIVE_",
  "NOTION_",
  "GITHUB_",
  "SLACK_",
];

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: responseHeaders(env) });
    }

    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "sankofa-agent-gateway" }, 200, env);
    }

    if (!isAuthorized(request, env)) {
      return json({ error: "unauthorized" }, 401, env);
    }

    const contentLength = Number(request.headers.get("content-length") ?? "0");
    if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) {
      return json({ error: "request_too_large" }, 413, env);
    }

    if (request.method === "POST" && url.pathname === "/v1/connect") {
      return connectAccount(request, env);
    }

    const toolPrefix = "/v1/tools/execute/";
    if (request.method === "POST" && url.pathname.startsWith(toolPrefix)) {
      const toolSlug = decodeURIComponent(url.pathname.slice(toolPrefix.length)).toUpperCase();
      return executeTool(request, env, toolSlug);
    }

    return json({ error: "not_found" }, 404, env);
  },
};

async function connectAccount(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request, env);
  if (body instanceof Response) return body;

  const userId = readString(body, "userId");
  const authConfigId = readString(body, "authConfigId");
  const callbackUrl = readOptionalString(body, "callbackUrl");
  if (!userId || !authConfigId) {
    return json({ error: "userId_and_authConfigId_are_required" }, 400, env);
  }

  const payload: JsonRecord = {
    user_id: userId,
    auth_config_id: authConfigId,
  };
  if (callbackUrl) payload.callback_url = callbackUrl;

  const upstream = await composioFetch(env, "/api/v3/connected_accounts/link", payload);
  return relay(upstream, env);
}

async function executeTool(request: Request, env: Env, toolSlug: string): Promise<Response> {
  if (!/^[A-Z0-9_]+$/.test(toolSlug)) {
    return json({ error: "invalid_tool_slug" }, 400, env);
  }

  const prefixes = (env.ALLOWED_TOOL_PREFIXES ?? DEFAULT_PREFIXES.join(","))
    .split(",")
    .map((value) => value.trim().toUpperCase())
    .filter(Boolean);
  if (!prefixes.some((prefix) => toolSlug.startsWith(prefix))) {
    return json({ error: "tool_not_allowed", tool_slug: toolSlug }, 403, env);
  }

  const body = await readJson(request, env);
  if (body instanceof Response) return body;

  const userId = readString(body, "userId");
  const connectedAccountId = readOptionalString(body, "connectedAccountId");
  const version = readOptionalString(body, "version") ?? "latest";
  const args = body.arguments;
  const approved = body.approved === true;

  if (!userId || !isJsonObject(args)) {
    return json({ error: "userId_and_object_arguments_are_required" }, 400, env);
  }

  const risk = classifyTool(toolSlug);
  if (risk === "DESTRUCTIVE") {
    return json(
      {
        error: "destructive_tool_blocked",
        tool_slug: toolSlug,
        risk,
      },
      403,
      env,
    );
  }

  if (risk !== "READ_ONLY" && !approved) {
    return json(
      {
        error: "approval_required",
        tool_slug: toolSlug,
        risk,
      },
      409,
      env,
    );
  }

  const payload: JsonRecord = {
    user_id: userId,
    version,
    arguments: args,
  };
  if (connectedAccountId) payload.connected_account_id = connectedAccountId;

  console.log(JSON.stringify({
    event: "tool_execute",
    tool_slug: toolSlug,
    risk,
    user_id: userId,
    approved,
  }));

  const upstream = await composioFetch(
    env,
    `/api/v3.1/tools/execute/${encodeURIComponent(toolSlug)}`,
    payload,
  );
  return relay(upstream, env);
}

function classifyTool(toolSlug: string): ToolRisk {
  const action = toolSlug.includes("_") ? toolSlug.slice(toolSlug.indexOf("_") + 1) : toolSlug;
  const destructive = ["DELETE_", "REMOVE_", "TRASH_", "CANCEL_", "REVOKE_", "PERMANENTLY_"];
  const readOnly = ["GET_", "LIST_", "SEARCH_", "READ_", "FETCH_", "FIND_", "QUERY_"];

  if (destructive.some((marker) => action.startsWith(marker))) return "DESTRUCTIVE";
  if (action.startsWith("DRAFT_") || action.includes("CREATE_DRAFT")) return "DRAFT";
  if (readOnly.some((marker) => action.startsWith(marker))) return "READ_ONLY";
  return "EXTERNAL_WRITE";
}

async function composioFetch(env: Env, path: string, payload: JsonRecord): Promise<Response> {
  if (!env.COMPOSIO_API_KEY) {
    return new Response(JSON.stringify({ error: "gateway_not_configured" }), {
      status: 503,
      headers: { "content-type": "application/json" },
    });
  }
  const base = (env.COMPOSIO_BASE_URL ?? DEFAULT_COMPOSIO_BASE).replace(/\/$/, "");
  return fetch(`${base}${path}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": env.COMPOSIO_API_KEY,
    },
    body: JSON.stringify(payload),
  });
}

async function readJson(request: Request, env: Env): Promise<JsonRecord | Response> {
  try {
    const body = await request.json<unknown>();
    return isJsonObject(body) ? body : json({ error: "json_object_required" }, 400, env);
  } catch {
    return json({ error: "invalid_json" }, 400, env);
  }
}

function isJsonObject(value: unknown): value is JsonRecord {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readString(body: JsonRecord, key: string): string | null {
  const value = body[key];
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function readOptionalString(body: JsonRecord, key: string): string | null {
  return readString(body, key);
}

function isAuthorized(request: Request, env: Env): boolean {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "") ?? "";
  return Boolean(env.SANKOFA_GATEWAY_TOKEN) && safeEqual(token, env.SANKOFA_GATEWAY_TOKEN);
}

function safeEqual(left: string, right: string): boolean {
  const encoder = new TextEncoder();
  const a = encoder.encode(left);
  const b = encoder.encode(right);
  if (a.length !== b.length) return false;
  let difference = 0;
  for (let index = 0; index < a.length; index += 1) {
    difference |= a[index] ^ b[index];
  }
  return difference === 0;
}

async function relay(upstream: Response, env: Env): Promise<Response> {
  const headers = responseHeaders(env);
  headers.set("content-type", upstream.headers.get("content-type") ?? "application/json");
  return new Response(upstream.body, {
    status: upstream.status,
    headers,
  });
}

function json(payload: unknown, status: number, env: Env): Response {
  const headers = responseHeaders(env);
  headers.set("content-type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(payload), { status, headers });
}

function responseHeaders(env: Env): Headers {
  return new Headers({
    "access-control-allow-origin": env.ALLOWED_ORIGIN ?? "https://sankofa-mini-pc-android.vercel.app",
    "access-control-allow-headers": "authorization, content-type",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "referrer-policy": "no-referrer",
  });
}
