'use client';
import { useEffect, useState } from 'react';
import { RouterProvider } from 'react-router-dom';
import { makeRouter } from './routes/router';
export default function Workspace(){
 const [router,setRouter]=useState<ReturnType<typeof makeRouter>|null>(null);
 useEffect(()=>{const instance=makeRouter();setRouter(instance);return()=>instance.dispose();},[]);
 return router?<RouterProvider router={router}/>:<div className="login" role="status">Loading ProjectSphere…</div>;
}
