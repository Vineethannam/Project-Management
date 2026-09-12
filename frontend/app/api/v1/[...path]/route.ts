// Same-origin BFF: preserves Spring's session and CSRF protections.
async function proxy(request: Request) {
    const base = process.env.SPRING_API_URL || 'https://project-project-management-api.onrender.com';
    if (!base)
        return Response.json({ message: 'The workspace service is not configured. Contact your administrator.' }, { status: 503 });
    const url = new URL(request.url);
    const target = new URL(url.pathname + url.search, base);
    const headers = new Headers();
    for (const key of ['content-type', 'cookie', 'x-csrf-token', 'accept']) {
        const v = request.headers.get(key);
        if (v)
            headers.set(key, v);
    }
    try {
        const upstream = await fetch(target, { method: request.method, headers, body: ['GET', 'HEAD'].includes(request.method) ? undefined : await request.arrayBuffer(), redirect: 'manual' });
        const out = new Headers();
        for (const key of ['content-type', 'content-disposition', 'set-cookie']) {
            const value = upstream.headers.get(key);
            if (value)
                out.set(key, value);
        }
        out.set('cache-control', 'no-store');
        return new Response(upstream.body, { status: upstream.status, headers: out });
    }
    catch {
        return Response.json({ message: 'The backend is unavailable. Your changes have not been saved.' }, { status: 502 });
    }
}
export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;

export const PATCH = proxy;
