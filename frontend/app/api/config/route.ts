export function GET() { 
  const url = process.env.SPRING_API_URL || 'https://project-project-management-api.onrender.com';
  return Response.json({ connected: !!url }, { headers: { 'cache-control': 'no-store' } }); 
}
