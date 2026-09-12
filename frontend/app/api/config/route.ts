export function GET() { return Response.json({ connected: !!process.env.SPRING_API_URL }, { headers: { 'cache-control': 'no-store' } }); }
