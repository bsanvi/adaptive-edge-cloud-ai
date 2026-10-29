/** Same-origin research operations. No browser-side scoring or fake job results. */
export type OpsJob={id:string;kind:'EXPERIMENT'|'EXECUTION';taskId:string;status:string;runAt:string;createdAt:string;updatedAt:string;reportId:number|null;error:string|null;source:string;result?:Record<string,unknown>;request?:Record<string,unknown>};
const API='/api/v1/research/operations';
async function call<T>(url:string, init?:RequestInit):Promise<T>{
  const r=await fetch(url,{cache:'no-store',...init});
  if(!r.ok){const response=await r.text();let error=response.slice(0,300);try{error=JSON.parse(response).error||error;}catch{/* text error */}throw new Error(`${r.status}: ${error}`)}
  return await r.json() as T;
}
function adminHeaders(key:string):HeadersInit{return {'Content-Type':'application/json','X-P4-Operations-Key':key}}
export const operationsApi={
  jobs:()=>call<OpsJob[]>(`${API}/jobs`),
  job:(id:string,key:string)=>call<OpsJob>(`${API}/jobs/${encodeURIComponent(id)}`,{headers:adminHeaders(key)}),
  submit:(input:Record<string,unknown>,key:string)=>call<OpsJob>(`${API}/jobs`,{method:'POST',headers:adminHeaders(key),body:JSON.stringify(input)}),
  cancel:(id:string,key:string)=>call<OpsJob>(`${API}/jobs/${encodeURIComponent(id)}`,{method:'DELETE',headers:adminHeaders(key)}),
  importImage:async (params:{file:File; datasetId:string;name:string;source:string;license:string;split:string;label:string;key:string})=>{
    const form=new FormData(); form.set('image',params.file);for(const k of ['datasetId','name','source','license','split','label'] as const)form.set(k,params[k]);
    return call<Record<string,unknown>>('/api/v1/research/datasets/admin/import',{method:'POST',headers:{'X-P4-Operations-Key':params.key},body:form});
  }
};
