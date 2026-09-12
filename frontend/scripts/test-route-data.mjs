import assert from 'node:assert/strict';
import {readFile,mkdir,writeFile,rm} from 'node:fs/promises';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
import ts from 'typescript';
import {createMemoryRouter} from 'react-router-dom';
const root=fileURLToPath(new URL('../',import.meta.url));
const output=path.join(root,'node_modules/.cache/projectsphere-route-tests');
await mkdir(output,{recursive:true});
for(const [name,source]of [['model','lib/projectsphere/model.ts'],['data','components/projectsphere/routes/data.ts']]){
 const code=(await readFile(path.join(root,source),'utf8')).replace('@/lib/projectsphere/model','./model.mjs');
 await writeFile(path.join(output,name+'.mjs'),ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ES2022}}).outputText);
}
const data=await import(pathToFileURL(path.join(output,'data.mjs')));const calls=[];let deny=false,stale=false;
const original=globalThis.fetch;
globalThis.fetch=async(url,init={})=>{
 calls.push({url,method:init.method||'GET',headers:new Headers(init.headers)});
 if(url.endsWith('/auth/csrf'))return Response.json({token:'test-csrf'});
 if(deny)return Response.json({message:'Sign in'},{status:401});
 if(stale&&init.method==='PATCH')return Response.json({message:'Record changed'},{status:409});
 if(url.includes('/progress'))return Response.json({id:'t1',status:'Review',version:1});
 if(url.endsWith('/tasks/t1'))return Response.json({id:'t1',title:'A task',version:0});
 if(url.endsWith('/projects')&&init.method==='POST')return Response.json({id:'p-new'});
 return Response.json({items:[],page:0,size:25,hasNext:false});
};
const router=createMemoryRouter([
 {id:'tasks',path:'/tasks',loader:data.listLoader('tasks')},
 {id:'projects',path:'/projects',loader:data.listLoader('projects')},
 {id:'detail',path:'/tasks/:id',loader:data.recordLoader('tasks'),action:data.recordAction('tasks','detail')},
 {path:'/login'}],{initialEntries:['/tasks']});
async function settled(){for(let i=0;i<200;i++){if(router.state.initialized&&router.state.navigation.state==='idle'&&[...router.state.fetchers.values()].every(f=>f.state==='idle'))return;await new Promise(r=>setTimeout(r,5));}throw Error('Router did not settle');}
try{
 await settled();assert.deepEqual(calls.map(c=>c.url),['/api/v1/tasks']);
 calls.length=0;await router.navigate('/projects');await settled();assert.deepEqual(calls.map(c=>c.url),['/api/v1/projects']);assert.equal(router.state.loaderData.projects.kind,'projects');
 calls.length=0;await router.navigate('/tasks?page=2&size=10');await settled();assert.equal(calls[0].url,'/api/v1/tasks?page=2&size=10');
 calls.length=0;await router.navigate('/tasks/t1');await settled();assert.equal(calls[0].url,'/api/v1/tasks/t1');assert.equal(router.state.loaderData.detail.item.id,'t1');
 calls.length=0;await router.fetch('progress','detail','/tasks/t1',{formMethod:'post',formEncType:'application/json',body:{intent:'progress',body:{status:'Review',version:0}}});await settled();
 const update=calls.find(c=>c.method==='PATCH');assert.equal(update.url,'/api/v1/tasks/t1/progress');assert.equal(update.headers.get('X-CSRF-TOKEN'),'test-csrf');assert(calls.some(c=>c.url==='/api/v1/tasks/t1'&&c.method==='GET'));assert(!calls.some(c=>c.url.includes('/projects')));
 stale=true;let staleData;const unsubscribe=router.subscribe(state=>{const value=state.fetchers.get('stale')?.data;if(value)staleData=value;});await router.fetch('stale','detail','/tasks/t1',{formMethod:'post',formEncType:'application/json',body:{intent:'progress',body:{status:'Review',version:0}}});await settled();assert.equal(staleData.error,'Record changed');unsubscribe();stale=false;
 const create=await data.recordAction('projects','create')({params:{},request:new Request('http://test/projects/new',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({body:{title:'New project'}})})});assert.equal(create.headers.get('Location'),'/projects/p-new');
 deny=true;await router.navigate('/projects');await settled();assert.equal(router.state.location.pathname,'/login');
 assert(!calls.some(c=>c.url.includes('/state')||c.url.includes('/entities/')));
 console.log('9 route-data checks passed: navigation, pagination, details, fetcher mutation, CSRF/revalidation, error handling, create redirect, login redirect, no bulk endpoint.');
}finally{router.dispose();globalThis.fetch=original;await rm(output,{recursive:true,force:true});}
