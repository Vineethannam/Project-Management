'use client';
import { createBrowserRouter } from 'react-router-dom';
import { modules } from '@/lib/projectsphere/model';
import { Shell, ModulePage, Dashboard, Workload, Hierarchy, EditorPage, DetailPage, AuthPage, RouteError } from './screens';
import * as data from './data';
export function makeRouter(){
 const children:any[]=[
  {index:true,loader:data.dashboardLoader,Component:Dashboard},
  {path:'overview',loader:data.dashboardLoader,Component:Dashboard},
  {path:'reports',loader:data.workloadLoader,Component:Workload},
  {path:'workload',loader:data.workloadLoader,Component:Workload},
  {path:'hierarchy',loader:data.hierarchyLoader,Component:Hierarchy},
  {path:'change-password',loader:data.authLoader,action:data.authAction('change-password'),element:<AuthPage mode="change-password"/>},
 ];
 for(const {key:kind}of modules.filter(m=>m.key!=='reports'))children.push(
  {path:kind,loader:data.listLoader(kind),action:kind==='attendance'?data.attendanceAction:undefined,Component:ModulePage},
  {path:kind+'/new',loader:data.newLoader(kind),action:data.recordAction(kind,'create'),Component:EditorPage},
  {path:kind+'/:id',loader:data.recordLoader(kind),action:data.recordAction(kind,'detail'),Component:DetailPage},
  {path:kind+'/:id/edit',loader:data.recordLoader(kind),action:data.recordAction(kind,'edit'),Component:EditorPage}
 );
 return createBrowserRouter([
  ...(['login','forgot-password','reset-password']as const).map(mode=>({path:'/'+mode,loader:data.authLoader,action:data.authAction(mode),element:<AuthPage mode={mode}/>,errorElement:<RouteError/>})),
  {path:'/logout',action:data.authAction('logout')},
  {path:'/lookups/:source',loader:data.lookupLoader},
  {path:'/record-comments',loader:data.commentsLoader},
  {path:'/record-files',loader:data.filesLoader},
  {path:'/related/:kind',loader:data.relatedLoader},
  {path:'/related/:kind/:id',loader:data.relatedLoader},
  {path:'/',id:'workspace',loader:data.sessionLoader,shouldRevalidate:()=>true,Component:Shell,errorElement:<RouteError/>,children:children.map(route=>({...route,errorElement:<RouteError/>}))},
  {path:'*',element:<RouteError message="This page does not exist."/>}
 ]);
}
