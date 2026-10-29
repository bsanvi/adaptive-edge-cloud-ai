/* Real observations only: edge metrics are polled via the Java backend.
   No mock random series, no simulated CPU/network measurements. */
import {$} from './common.js';

const MAX_POINTS=36;
const POLL_MS=8000;
const points=[];
const serviceNames=['Dashboard','Edge','Decision','Cloud','Estimator'];
const format=(n,decimals=1)=>Number.isFinite(n)?Number(n).toFixed(decimals):'—';
const number=x=>typeof x==='number'&&Number.isFinite(x)?x:null;

function updateServices(services){
 const root=$('services');if(!root)return;
 root.replaceChildren();
 let online=0;
 for(const name of serviceNames){
   const yes=services?.[name]===true;
   if(yes)online++;
   const item=document.createElement('div');item.className='service';
   const title=document.createElement('span');title.textContent=name;
   const indicator=document.createElement('span');indicator.className='chip '+(yes?'success':'failed');
   indicator.textContent=yes?'ONLINE':'OFFLINE';
   item.append(title,indicator);root.append(item);
 }
 $('liveServices').textContent=`${online}/5`;
}

function lastObserved(){
 return points.length?points[points.length-1].at:null;
}

function drawChart(){
 const canvas=$('telemetryChart');if(!canvas)return;
 const width=Math.max(280,canvas.clientWidth||500),height=218;
 const dpr=Math.min(2,window.devicePixelRatio||1);
 canvas.width=Math.round(width*dpr);canvas.height=Math.round(height*dpr);
 const c=canvas.getContext('2d');c.scale(dpr,dpr);
 const colors={cpu:'#56e2d0',ram:'#b59aff'},pad={l:34,r:14,t:16,b:26};
 const innerW=width-pad.l-pad.r,innerH=height-pad.t-pad.b;
 c.clearRect(0,0,width,height);
 c.font='11px Segoe UI, Arial';
 c.textBaseline='middle';
 for(const tick of [0,25,50,75,100]){
  const y=pad.t+innerH*(1-tick/100);
  c.strokeStyle='#2e4158';c.lineWidth=1;c.beginPath();c.moveTo(pad.l,y);c.lineTo(width-pad.r,y);c.stroke();
  c.fillStyle='#9aaec9';c.fillText(tick+'%',1,y);
 }
 if(!points.length){c.fillStyle='#9aaec9';c.font='13px Segoe UI, Arial';c.fillText('Waiting for measured Edge values…',pad.l+15,pad.t+innerH/2);return;}
 for(const key of ['cpu','ram']){
  c.strokeStyle=colors[key];c.fillStyle=colors[key];c.lineWidth=2.6;c.lineJoin='round';
  c.beginPath();
  points.forEach((point,i)=>{
   const x=pad.l+((points.length===1?0.5:i/(points.length-1))*innerW);
   const y=pad.t+(1-Math.min(100,Math.max(0,point[key]))/100)*innerH;
   if(i===0)c.moveTo(x,y);else c.lineTo(x,y);
  });
  c.stroke();
  if(points.length===1){const y=pad.t+(1-Math.min(100,Math.max(0,points[0][key]))/100)*innerH;c.beginPath();c.arc(pad.l+innerW/2,y,4,0,2*Math.PI);c.fill();}
 }
 c.fillStyle='#9aaec9';c.font='11px Segoe UI, Arial';
 c.textBaseline='alphabetic';
 c.fillText(points.length>=2?new Date(points[0].at).toLocaleTimeString():'First observation',pad.l,height-6);
 const right=new Date(lastObserved()).toLocaleTimeString();
 c.fillText(right,Math.max(pad.l,width-pad.r-c.measureText(right).width),height-6);
}

function renderNetwork(data){
 const n=data?.lastNetwork;
 const connected=n?.connected===true;
 $('liveLatency').textContent=connected&&number(n.latencyMs)!==null?format(n.latencyMs,2)+' ms':'—';
 $('liveBandwidth').textContent=connected&&number(n.bandwidthMbps)!==null?format(n.bandwidthMbps,2)+' Mbps':'—';
 const provenance=$('networkProvenance');
 if(!n){provenance.textContent='No stored network probe yet. Run a task to collect one.';return;}
 provenance.textContent=`Previous task ${n.taskId||'—'} (${n.observedAt||'time unavailable'}). Approximate HTTP probe measured during execution; NOT a continuously updated network measurement.`;
}

function renderDevice(data){
 const device=data?.device;
 const cpu=number(device?.cpuPercent),ram=number(device?.ramPercent),battery=number(device?.batteryPercent);
 const ok=data?.deviceAvailable===true&&cpu!==null&&ram!==null;
 $('liveCpu').textContent=ok?format(cpu)+'%':'—';
 $('liveRam').textContent=ok?format(ram)+'%':'—';
 $('liveBattery').textContent=ok&&battery!==null?format(battery)+'%':'—';
 $('liveCpuNote').textContent=ok?'From /edge/metrics':'Edge data unavailable';
 $('liveRamNote').textContent=ok?'From /edge/metrics':'Edge data unavailable';
 $('liveBatteryNote').textContent=!ok?'Edge data unavailable':battery===null?'Battery not reported':device.powerPlugged===true?'Power connected':device.powerPlugged===false?'On battery':'Power status unknown';
 $('telemetryBadge').className='chip '+(ok?'success':'failed');
 $('telemetryBadge').textContent=ok?'LIVE · MEASURED':'TELEMETRY OFFLINE';
 $('telemetryFreshness').textContent=ok?'Edge sampled at '+new Date(data.sampledAt).toLocaleTimeString():'Last attempt '+new Date(data.sampledAt).toLocaleTimeString()+' · Edge values unavailable';
 if(ok){
   // Add only real, newly polled readings; never fill gaps or interpolate samples.
   points.push({at:data.sampledAt,cpu,ram});
   if(points.length>MAX_POINTS)points.shift();
   $('telemetryPoints').textContent=points.length+' measured point'+(points.length===1?'':'s');
   $('downloadTelemetry').disabled=points.length===0;
   const btn=$('useLiveCpu');
   btn.disabled=points.length<8;
   btn.textContent=`Use live CPU data (${Math.min(points.length,8)}/8)`;
 }
 drawChart();
}

export function renderRouteMix(rows){
 const host=$('routeMix');if(!host)return;
 const runs=rows.filter(r=>r.STATUS==='SUCCESS'&&['EDGE','CLOUD','HYBRID'].includes(r.EXECUTED_ROUTE));
 host.replaceChildren();
 if(!runs.length){host.textContent='No successful executions yet.';return;}
 for(const name of ['EDGE','CLOUD','HYBRID']){
  const count=runs.filter(r=>r.EXECUTED_ROUTE===name).length;
  const item=document.createElement('div');item.className='mix-row';
  const label=document.createElement('div');label.className='mix-head';
  const nameEl=document.createElement('span');nameEl.textContent=name;
  const value=document.createElement('strong');value.textContent=`${count} / ${runs.length}`;
  label.append(nameEl,value);
  const track=document.createElement('div');track.className='bartrack';
  const fill=document.createElement('div');fill.className='barfill '+name.toLowerCase();
  fill.style.width=(100*count/runs.length)+'%';track.append(fill);
  item.append(label,track);host.append(item);
 }
}

export function startTelemetry(){
 const exportBtn=$('downloadTelemetry');
 if(exportBtn){exportBtn.onclick=()=>{
   if(!points.length)return;
   const lines=['observed_at_utc,cpu_percent,ram_percent,source',...points.map(p=>`${p.at},${p.cpu},${p.ram},MEASURED_EDGE_METRICS`)];
   const blob=new Blob([lines.join('\r\n')+'\r\n'],{type:'text/csv;charset=utf-8'});
   const url=URL.createObjectURL(blob);
   const link=document.createElement('a');link.href=url;
   link.download='edgecloud-measured-telemetry-'+new Date().toISOString().replaceAll(':','-')+'.csv';
   document.body.appendChild(link);link.click();link.remove();
   setTimeout(()=>URL.revokeObjectURL(url),1000);
 };}
 const live=$('useLiveCpu');
 if(live){live.onclick=()=>{
   if(points.length<8)return;
   const newest=points.slice(-8);
   $('samples').value=newest.map(x=>x.cpu.toFixed(4)).join(', ');
   $('sampleSource').textContent=`Input: 8 actually measured Edge CPU percentages (from ${new Date(newest[0].at).toLocaleTimeString()} to ${new Date(newest[7].at).toLocaleTimeString()}). Reference model only; not a trained CPU anomaly detector.`;
   $('samples').scrollIntoView({behavior:'smooth',block:'center'});
 };}
 $('samples')?.addEventListener('input',()=>{$('sampleSource').textContent='Input: user-entered samples (source not independently verified).'});
 let timer=null,fetching=false;
 async function poll(){
   if(fetching)return;
   if(document.hidden){timer=setTimeout(poll,POLL_MS);return;}
   fetching=true;
   try{
    const controller=new AbortController();
    const timeout=setTimeout(()=>controller.abort(),13000);
    let response;
    try{response=await fetch('/api/v1/integrated/telemetry',{cache:'no-store',signal:controller.signal});}
    finally{clearTimeout(timeout);}
    if(!response.ok)throw new Error(`Telemetry API HTTP ${response.status}; restart the upgraded Dashboard Java backend.`);
    const data=await response.json();
    updateServices(data.services);
    renderDevice(data);
    renderNetwork(data);
    $('telemetryError').textContent='Values are retrieved from the running Java and Edge services, not generated by the dashboard.';
   }catch(e){
    $('telemetryBadge').className='chip failed';$('telemetryBadge').textContent='TELEMETRY UNAVAILABLE';
    for(const key of ['liveCpu','liveRam','liveBattery','liveServices'])$(key).textContent='—';
    $('telemetryFreshness').textContent='Live polling interrupted. Previous chart points are historical.';
    $('telemetryError').textContent='Cannot retrieve real measurements: '+e.message;
   }finally{fetching=false;timer=setTimeout(poll,POLL_MS);}
 }
 // Resize uses real observed data only. No new measurements are synthesized.
 window.addEventListener('resize',drawChart);
 poll();
}
