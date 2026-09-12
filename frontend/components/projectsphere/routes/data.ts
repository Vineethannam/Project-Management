import { redirect, type ActionFunctionArgs, type LoaderFunctionArgs } from 'react-router-dom';
import { modules } from '@/lib/projectsphere/model';
export class ApiFailure extends Error { constructor(public status:number,message:string){super(message);} }
export async function api(path:string, init:RequestInit={}, signal?:AbortSignal):Promise<any> {
 const method=init.method||'GET';const headers=new Headers(init.headers);
 if(!['GET','HEAD'].includes(method)){
  const cr=await fetch('/api/v1/auth/csrf',{signal,cache:'no-store'});
  if(!cr.ok)throw new ApiFailure(cr.status,'Unable to establish a secure session. Please retry.');
  const token:any=await cr.json();headers.set('X-CSRF-TOKEN',token.token);
 }
 if(init.body&&!(init.body instanceof FormData))headers.set('Content-Type','application/json');
 const res=await fetch('/api/v1'+path,{...init,headers,signal,cache:'no-store'});
 const body:any=await res.json().catch(()=>({message:'The service returned an invalid response.'}));
 if(!res.ok)throw new ApiFailure(res.status,body.message||'Request failed');return body;
}
export async function load(path:string,request:Request){
 try{return await api(path,{},request.signal);}catch(e){if(e instanceof ApiFailure){if(e.status===401)throw redirect('/login');throw new Response(e.message,{status:e.status});}throw e;}
}
export async function authLoader({request}:LoaderFunctionArgs){
 try{const res=await fetch('/api/config',{signal:request.signal});const config:any=await res.json();return {configured:!!config.connected};}catch{return {configured:false};}
}
export async function sessionLoader({request}:LoaderFunctionArgs){return load('/auth/session',request);}
export function listLoader(kind:string){return async({request}:LoaderFunctionArgs)=>{
 const query=new URL(request.url).search;const list=await load('/'+kind+query,request);
 const current=kind==='attendance'?await load('/attendance/current',request):null;
 return {kind,list,current};
};}
export function recordLoader(kind:string){return async({request,params}:LoaderFunctionArgs)=>({kind,item:await load('/'+kind+'/'+encodeURIComponent(params.id!),request)});}
export function newLoader(kind:string){return ()=>({kind,item:null});}
export async function dashboardLoader({request}:LoaderFunctionArgs){return load('/dashboard/summary',request);}
export async function workloadLoader({request}:LoaderFunctionArgs){return load('/reports/workload'+new URL(request.url).search,request);}
export async function hierarchyLoader({request}:LoaderFunctionArgs){const q=new URL(request.url).searchParams;if(!q.has('reportingTo'))q.set('reportingTo','');return load('/users?'+q,request);}
export async function lookupLoader({params,request}:LoaderFunctionArgs){if(!modules.some(m=>m.key===params.source))throw new Response('Not found',{status:404});return load('/'+params.source+'/options'+new URL(request.url).search,request);}
export async function commentsLoader({request}:LoaderFunctionArgs){return load('/comments'+new URL(request.url).search,request);}
export async function filesLoader({request}:LoaderFunctionArgs){return load('/files'+new URL(request.url).search,request);}
export async function relatedLoader({request,params}:LoaderFunctionArgs){if(!['courses','enrollments'].includes(params.kind!))throw new Response('Not found',{status:404});return load('/'+params.kind+(params.id?'/'+encodeURIComponent(params.id):'')+new URL(request.url).search,request);}
async function actionError(error:unknown){if(error instanceof ApiFailure&&error.status===401)throw redirect('/login');return {error:error instanceof Error?error.message:'Unable to complete this action.'};}
export function authAction(action:'login'|'forgot-password'|'reset-password'|'change-password'|'logout'){
 return async({request}:ActionFunctionArgs)=>{try{
  const body=Object.fromEntries(await request.formData());
  if(['reset-password','change-password'].includes(action)&&body.newPassword!==body.confirmPassword)return {error:'Passwords do not match.'};
  delete body.confirmPassword;
  const result=await api('/auth/'+action,{method:'POST',body:JSON.stringify(body)},request.signal);
  if(action==='login')return redirect('/overview');if(action==='logout')return redirect('/login');
  return {message:result.message};
 }catch(e){if(action==='login'&&e instanceof ApiFailure&&e.status===401)return {error:e.message};return actionError(e);}};
}
export function recordAction(kind:string,mode:'create'|'edit'|'detail'){
 return async({request,params}:ActionFunctionArgs)=>{try{
  const id=encodeURIComponent(params.id||'');
  if(request.headers.get('content-type')?.includes('multipart/form-data')){
   const form=await request.formData();form.set('entityKind',kind);form.set('entityId',params.id!);
   await api('/files',{method:'POST',body:form},request.signal);return {message:'File uploaded.'};
  }
  const payload:any=await request.json();const body=payload.body||payload;const intent=payload.intent||mode;
  if(intent==='delete'){await api('/'+kind+'/'+id+'?version='+encodeURIComponent(body.version),{method:'DELETE'},request.signal);return redirect('/'+kind);}
  let path='/'+kind+(mode==='create'?'':'/'+id),method=mode==='create'?'POST':'PUT';
  if(mode==='detail'){
   const commands:Record<string,{path:string;method:string}>={
    progress:{path:'/progress',method:'PATCH'},assignee:{path:'/assignee',method:'PATCH'},permissions:{path:'/permissions',method:'PUT'},
    submit:{path:'/submit',method:'POST'},evaluate:{path:'/evaluate',method:'POST'},read:{path:'/read',method:'POST'},
    lesson:{path:'/lessons/'+encodeURIComponent(payload.lesson)+'/complete',method:'POST'}
   };
   const permitted:Record<string,string[]>={tasks:['progress','assignee'],bugs:['progress','assignee'],roles:['permissions'],assignments:['submit','evaluate'],assessments:['submit'],notifications:['read'],enrollments:['lesson']};
   if(!permitted[kind]?.includes(intent))return {error:'Unsupported action.'};
   path+=commands[intent].path;method=commands[intent].method;
  }
  const result=await api(path,{method,body:JSON.stringify(body)},request.signal);
  if(mode!=='detail'&&kind!=='comments')return redirect('/'+kind+'/'+result.id);
  return {message:'Saved successfully.',record:result};
 }catch(e){return actionError(e);}};
}
export async function attendanceAction({request}:ActionFunctionArgs){try{
 const form=await request.formData();const intent=String(form.get('intent'));
 if(!['check-in','check-out','break-start','break-end'].includes(intent))return {error:'Unknown attendance action.'};
 await api('/attendance/'+intent,{method:'POST',body:'{}'},request.signal);return {message:'Attendance updated.'};
}catch(e){return actionError(e);}}
