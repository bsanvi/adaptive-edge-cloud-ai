import { z } from 'zod';

export type J = Record<string, unknown>;
export type Route = 'EDGE' | 'CLOUD' | 'HYBRID';
export type Run = {
  reportId?:number|null; taskId: string; status: string; selectedRoute: Route | null; executedRoute: Route | null;
  fallbackUsed: boolean; prediction: string | null; confidence: number | null;
  totalTimeMs: number; explanation: string; error: string | null;
  decision: J; estimates: J; network: J;
};
export type Analysis = {analysisId:string;expiresAt:string;report:Run};
export type Telemetry = { sampledAt: string; deviceAvailable: boolean; device?: {
  cpuPercent: number; ramPercent: number; ramUsedMb?:number|null; ramAvailableMb?:number|null; batteryPercent: number|null;
  powerPlugged: boolean|null; edgeTimestamp: string|null; source: string
} | null; services?: Record<string,boolean>; lastNetwork?: J|null; lastRun?: J|null; source: string };
export type HistoryRow = { DATA_SOURCE?: string;WORKLOAD_TYPE?:string;TASK_NAME?:string; ID: number; TASK_ID: string; CREATED_AT: string; STATUS: string; SELECTED_ROUTE: string|null; EXECUTED_ROUTE: string|null; TOTAL_TIME_MS: number; PREDICTION?:string; CONFIDENCE?:number; FALLBACK_USED?:boolean; ESTIMATED_LATENCY_MS?:number|null; ESTIMATED_ENERGY_J?:number|null; ESTIMATED_CARBON_G?:number|null; ESTIMATED_TRANSFER_MB?:number|null; NETWORK_SOURCE?:string|null; NETWORK_LATENCY_MS?:number|null; NETWORK_BANDWIDTH_MBPS?:number|null };
export const ROUTES: Route[] = ['EDGE','CLOUD','HYBRID'];
export const runSchema = z.object({ taskId:z.string().min(1).max(100), samples:z.array(z.number().finite()).min(8).max(4096), deadlineMs:z.number().positive().max(10_000_000), privacyRequired:z.boolean(), complexity:z.enum(['LOW','MEDIUM','HIGH']), previousRoute:z.enum(['EDGE','CLOUD','HYBRID']).nullable().optional() });
const BASE='/api/v1/integrated';
export async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController(); const timeout = setTimeout(()=>controller.abort(), 90000);
  try {
    const res = await fetch(url,{ ...init, signal: controller.signal, headers: init?.body instanceof FormData ? init.headers : { 'Accept':'application/json', ...((init?.body && {'Content-Type':'application/json'}) || {}),...init?.headers }, cache:'no-store' });
    if (!res.ok) {const body=await res.text(); let detail=body.slice(0,280);try{const parsed=JSON.parse(body);detail=parsed.error||parsed.message||detail;}catch{/* plain response */}throw new Error(`${res.status} ${detail || res.statusText}`)}
    return await res.json() as T;
  } finally {clearTimeout(timeout)}
}
export const api = {
  telemetry: ()=>request<Telemetry>(`${BASE}/telemetry`),
  liveNetwork: ()=>request<{source:string;sampledAt:string;connected:boolean;latencyMs:number|null;bandwidthMbps:number|null;probeBytes:number}>('/api/v1/research/network/live'),
  services: ()=>request<Record<string,boolean>>(`${BASE}/services`).then(raw=>Object.fromEntries(Object.entries(raw).map(([k,v])=>[k.toLowerCase(),v]))),
  health: ()=>request<J>(`${BASE}/health`),
  history: (limit=100)=>request<HistoryRow[]>(`/api/v1/research/history?limit=${limit}`),
  analytics: ()=>request<{source:string;total:number;success:number;failed:number;edge:number;cloud:number;hybrid:number;fallback:number;averageTotalMs:number|null}>('/api/v1/research/analytics'),
  report: (id: number)=>request<Run>(`${BASE}/report/${id}`),
  analyze: (data: z.input<typeof runSchema>)=>request<Analysis>(`${BASE}/analyze`,{method:'POST',body:JSON.stringify(runSchema.parse(data))}),
  analysis: (id:string)=>request<Analysis>(`${BASE}/analysis/${encodeURIComponent(id)}`),
  executeAnalysis: (id:string)=>request<Run>(`${BASE}/analysis/${encodeURIComponent(id)}/execute`,{method:'POST'}),
  execute: (data: z.input<typeof runSchema>)=>request<Run>(`${BASE}/execute`,{method:'POST',body:JSON.stringify(runSchema.parse(data))}),
  uploadImage:(file:File, args: {taskId:string; deadlineMs:number;privacyLevel:string;priorityProfile:string})=>{
    const body=new FormData(); body.set('image',file);body.set('taskId',args.taskId);body.set('deadlineMs',String(args.deadlineMs));body.set('privacyLevel',args.privacyLevel);body.set('priorityProfile',args.priorityProfile);
    return request<ImageProfile>('/api/v1/research/images',{method:'POST',body});
  },
  classifyImage:(ref:string)=>request<J>(`/api/v1/research/images/${encodeURIComponent(ref)}/classify`,{method:'POST'}),
  scenario:(body:J)=>request<ScenarioResult>('/api/v1/research/experiments/evaluate',{method:'POST',body:JSON.stringify(body)}),
  catalogue:()=>request<DatasetManifest[]>('/api/v1/research/datasets'),
  datasetSamples:(id:string)=>request<DatasetSample[]>(`/api/v1/research/datasets/${encodeURIComponent(id)}/samples`),
  metadata:(taskId:string, data:J)=>request<J>(`/api/v1/research/metadata/${encodeURIComponent(taskId)}`,{method:'PUT',body:JSON.stringify(data)}),
  readMetadata:(taskId:string)=>request<J>(`/api/v1/research/metadata/${encodeURIComponent(taskId)}`),
};
export type ImageProfile = {ref:string;taskId:string;profile: J; fileName:string; fileSizeBytes:number; profilingTimestamp:string; profileSource:string};
export type ScenarioResult={decision:J;estimates:J;network:J;device:J;source:string;sampledAt:string};
export type DatasetSample={fileName:string;sizeBytes:number;previewUrl:string;label:string};
export type DatasetManifest={id:string;name:string;source:string;description:string;taskType:string;samples:number;labels:string[];license:string;split:string;lastImported:string|null;modelCompatibility:string;available:boolean};
export const obj=(v:unknown):J=>(v!==null && typeof v==='object' && !Array.isArray(v)? v as J : {});
export const num=(v:unknown):number|null=>(typeof v==='number' && Number.isFinite(v)?v:null);
export const fmt=(v:unknown,digits=1)=>{const n=num(v);return n===null?'—':n.toLocaleString(undefined,{maximumFractionDigits:digits})};
export const fmtDate=(v:unknown)=>{if(!v)return '—';const d=new Date(String(v));return Number.isNaN(d.getTime())?String(v):d.toLocaleString()};
export const asText=(v:unknown)=>v===null || v===undefined || v===''?'—':String(v);
export const keys=(v:J)=>Object.entries(v);
export function lastReportForTask(rows:HistoryRow[],taskId:string){return rows.find(r=>r.TASK_ID===taskId)}
export function extractEstimates(run:Run):{route:Route;available:boolean;time:number|null;energy:number|null;carbon:number|null;transfer:number|null}[]{return ROUTES.map(route=>{const e=obj(run.estimates?.[route]);return {route,available:e.estimateAvailable===true,time:num(e.estimatedLatencyMs),energy:num(e.energyEstimateJ),carbon:num(e.carbonEstimateG),transfer:num(e.communicationMb)}})}
export function safeDownload(data:string,name:string,type='application/json') {const url=URL.createObjectURL(new Blob([data],{type}));const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
export function csv(rows:Record<string,unknown>[]) {if(!rows.length)return '';const columns=[...new Set(rows.flatMap(r=>Object.keys(r)))];return [columns.join(','),...rows.map(r=>columns.map(c=>'"'+String(r[c]??'').replaceAll('"','""')+'"').join(','))].join('\r\n')}
