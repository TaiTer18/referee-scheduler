const API_BASE_URL = "http://localhost:8080";

type RequestOptions = RequestInit;

async function readErrorMessage(response: Response) {
    const contentType = response.headers.get("Content-Type") ?? "";

    if (contentType.includes("application/json")) {
        try {
            const payload = (await response.json()) as {
                message?: string;
                error?: string;
            };

            return (
                payload.message ?? payload.error ?? "Request failed with status " + response.status
            );
        } catch {
            return "Request failed with status " + response.status;
        }
    }

    const message = await response.text();
    return message || "Request failed with status " + response.status;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const headers = new Headers(options.headers);

    if (!headers.has("Content-Type") && options.body) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        const message = await readErrorMessage(response);
        throw new Error(message);
    }

    return (await response.json()) as T;
}
