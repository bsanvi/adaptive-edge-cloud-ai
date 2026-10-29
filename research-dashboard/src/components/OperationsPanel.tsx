import {useEffect,useState} from 'react';
import {useQuery,useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {Activity,CalendarClock,Download,HardDrive,Layers3,LockKeyhole,Play,RadioTower,Trash2} from 'lucide-react';
import {DataTable,ErrorNotice,Field,Panel,SourceTag} from './UI';
import {fmt,fmtDate,safeDownload,csv} from '../lib/api';
import {operationsApi,type OpsJob} from '../lib/operationsApi';

type Scenario={cpuPercent:number;ramPercent:number;batteryPercent:number;latencyMs:number;bandwidthMbps:number;deadlineMs:number;inputSizeBytes:number;edgeCapable:boolean;cloudCapable:boolean;privacyRequired:boolean;carbonGPerKwh:number};
export function OperationsPanel({scenario}:{scenario:Scenario}){
  const client=useQueryClient();
  const navigate=useNavigate();
  const jobs=useQuery({queryKey:['p4-operations'],queryFn:operationsApi.jobs,refetchInterval:5000,retry:0});
  const [stream,setStream]=useState('Connecting');
  const [metrics,setMetrics]=useState<Record<string,unknown>|null>(null);
  const [key,setKey]=useState(''); // intentionally never persisted or logged
  const [runAt,setRunAt]=useState('');
  const [samples,setSamples]=useState('');
  const [deadline,setDeadline]=useState(20000);
  const [privacy,setPrivacy]=useState(true);
  const [complexity,setComplexity]=useState<'LOW'|'MEDIUM'|'HIGH'>('LOW');
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [selected,setSelected]=useState<OpsJob|null>(null);
  useEffect(()=>{
    const events=new EventSource('/api/v1/research/operations/events');
    events.addEventListener('connected',()=>setStream('LIVE · server events'));
    events.addEventListener('job',()=>void client.invalidateQueries({queryKey:['p4-operations']}));
    events.onerror=()=>setStream('Reconnecting · history polling available');
    const telemetry=new EventSource('/api/v1/research/operations/telemetry/events');
    telemetry.addEventListener('telemetry',(evt:MessageEvent)=>{try{const data=JSON.parse(evt.data);if(data&&typeof data==='object')setMetrics(data)}catch{/* ignore corrupted frame */}});
    return ()=>{events.close();telemetry.close()};
  },[client]);
  async function submit(kind:'EXPERIMENT'|'EXECUTION'){
    setError('');setBusy(true);
    try{
      if(key.length<24)throw new Error('Enter your local operations key (at least 24 characters).');
      const newId=`${kind==='EXPERIMENT'?'exp':'exec'}-${Date.now()}`;
      const schedule=runAt?new Date(runAt).toISOString():undefined;
      let payload:Record<string,unknown>;
      if(kind==='EXPERIMENT')payload={kind,taskId:newId,runAt:schedule,scenario:{...scenario,taskId:newId}};
      else{
        const numbers=samples.split(/[\s,;]+/).filter(Boolean).map(Number);
        if(numbers.length<8||numbers.length>4096||numbers.some(n=>!Number.isFinite(n)))throw new Error('Paste 8–4096 finite sensor readings separated by commas or spaces.');
        payload={kind,taskId:newId,runAt:schedule,samples:numbers,deadlineMs:deadline,privacyRequired:privacy,complexity,previousRoute:null};
      }
      const created=await operationsApi.submit(payload,key);
      setSelected(created);
      await client.invalidateQueries({queryKey:['p4-operations']});
    }catch(e){setError(e instanceof Error?e.message:String(e))}finally{setBusy(false)}
  }
  async function cancel(id:string){setError('');try{await operationsApi.cancel(id,key);await client.invalidateQueries({queryKey:['p4-operations']})}catch(e){setError(e instanceof Error?e.message:String(e))}}
  function reviewQueued(){
    const req=selected?.request as Record<string,unknown>|undefined;
    if(!selected||!req||!Array.isArray(req.samples)){setError('Load the authorized queued job first.');return;}
    sessionStorage.setItem('p4-review-task',JSON.stringify(req));
    navigate('/');
  }
  async function view(id:string){setError('');try{setSelected(await operationsApi.job(id,key))}catch(e){setError(e instanceof Error?e.message:String(e))}}
  const device=metrics?.device as Record<string,unknown>|null|undefined;
  const list=jobs.data||[];
  return <div className="section-gap stack" id="research-operations">
    <Panel title="Task queue & scheduled experiments" overline="06 / PERSISTED H2 OPERATIONS" action={<SourceTag type="REAL BACKEND QUEUE"/>}>
      <p className="subtle">Queue real sensor executions or schedule simulations against the actual Decision Engine. Jobs survive restarts. A job interrupted while running is marked INTERRUPTED, never silently executed twice.</p>
      <div className="grid two section-gap">
        <div className="form-stack">
          <Field label="Operations key" hint="Required for creating, inspecting and cancelling jobs"><input className="input" type="password" autoComplete="off" value={key} onChange={e=>setKey(e.target.value)} placeholder="Local administrator key"/></Field>
          <Field label="Run at" hint="Optional · local time"><input className="input" type="datetime-local" value={runAt} onChange={e=>setRunAt(e.target.value)}/></Field>
          <button className="button button-primary" disabled={busy} onClick={()=>void submit('EXPERIMENT')}><CalendarClock size={16}/>{busy?'Submitting…':'Schedule current what-if experiment'}</button>
          <small className="muted">Uses the sliders above · SIMULATED INPUT → REAL ESTIMATOR + DECISION ENGINE. No model inference for experiment-only jobs.</small>
        </div>
        <div className="form-stack">
          <Field label="Sensor readings" hint="8–4096 comma-separated numbers"><textarea className="input" rows={3} value={samples} onChange={e=>setSamples(e.target.value)} placeholder="Paste actual sensor readings; none are invented"/></Field>
          <Field label="Deadline (ms)"><input className="input" type="number" min={1} max={10000000} value={deadline} onChange={e=>setDeadline(Number(e.target.value))}/></Field>
          <div className="grid two"><Field label="Complexity"><select className="select" value={complexity} onChange={e=>setComplexity(e.target.value as 'LOW'|'MEDIUM'|'HIGH')}><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></Field><label className="field"><span className="field-label">Privacy</span><span><input type="checkbox" checked={privacy} onChange={e=>setPrivacy(e.target.checked)}/> Edge required</span></label></div>
          <button className="button button-secondary" disabled={busy} onClick={()=>void submit('EXECUTION')}><Play size={16}/>Queue for analysis & review</button>
          <small className="muted">Human approval required. Open the queued task in Overview, inspect the real decision audit, then execute once using the existing workflow.</small>
        </div>
      </div>
      {error&&<ErrorNotice message={error}/>}<div className="notice info section-gap"><LockKeyhole size={15}/>Actions require P4_OPERATIONS_KEY. A blank key disables write endpoints. Do not expose this research prototype to the public Internet without proper authentication and rate limiting.</div>
    </Panel>
    <Panel title="Live queue events & Edge telemetry" overline="07 / SERVER-SENT EVENTS" action={<SourceTag type="LIVE BACKEND STREAM"/>}>
      <div className="grid four"><div className="metric"><div className="metric-top"><span>Queue stream</span><RadioTower size={16}/></div><strong>{stream}</strong></div><div className="metric"><div className="metric-top"><span>CPU</span><Activity size={16}/></div><div className="metric-value">{fmt(device?.cpuPercent)}%</div></div><div className="metric"><div className="metric-top"><span>RAM</span><HardDrive size={16}/></div><div className="metric-value">{fmt(device?.ramPercent)}%</div></div><div className="metric"><div className="metric-top"><span>Power</span><Layers3 size={16}/></div><div className="metric-value">{fmt(device?.batteryPercent)}%</div></div></div>
      <p className="tiny muted section-gap">{metrics?.sampledAt?`Observed ${fmtDate(metrics.sampledAt)} · ${String(device?.source||'EDGE UNAVAILABLE')}`:'Waiting for actual Edge readings; unavailable values are not simulated.'}</p>
    </Panel>
    <Panel title="Scheduled and completed jobs" overline="08 / RESTART-PERSISTENT RECORDS" action={<button className="button button-secondary button-sm" disabled={!list.length} onClick={()=>safeDownload(csv(list as unknown as Record<string,unknown>[]),'scheduled-research-jobs.csv','text/csv')}><Download size={14}/>CSV</button>}>
      {jobs.isError&&<ErrorNotice message={jobs.error.message} onRetry={()=>void jobs.refetch()}/>}<DataTable headers={['Scheduled','Task','Type','State','Report','Action']} rows={list.map(j=>[fmtDate(j.runAt),j.taskId,j.kind,<span className="mono">{j.status}</span>,j.reportId?<a href={`#/results?id=${j.reportId}`}>#{j.reportId}</a>:'—',<div className="page-actions"><button className="button button-secondary button-sm" onClick={()=>void view(j.id)}>Inspect</button>{j.status==='QUEUED'&&<button title="Cancel queued job" className="button button-secondary button-sm" disabled={key.length<24} onClick={()=>void cancel(j.id)}><Trash2 size={14}/></button>}</div>])}/>
      {selected&&<div className="section-gap"><div className="section-heading">{selected.taskId} · {selected.status}</div>{selected.status==='REVIEW_REQUIRED'&&<button className="button button-primary button-sm section-gap" onClick={reviewQueued}>Open in Overview for analysis and approval</button>}{selected.error&&<ErrorNotice message={selected.error}/>}<pre className="json-view"><code>{JSON.stringify(selected.result||selected.request||{id:selected.id,reportId:selected.reportId,kind:selected.kind,status:selected.status},null,2)}</code></pre></div>}
    </Panel>
  </div>
}
