export const $ = id => document.getElementById(id);
export const money = (n,d=2)=> typeof n==='number' && Number.isFinite(n)?n.toFixed(d):'—';
export const pct = n => typeof n==='number' && Number.isFinite(n)?(n*100).toFixed(1)+'%':'—';
export const esc = s=>String(s??'—').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;');
export const getJson=async path=>{const r=await fetch(path,{cache:'no-store'});if(!r.ok)throw Error(`${path}: HTTP ${r.status}`);return await r.json()};
export async function getRuns(){return getJson('/api/v1/integrated/history?limit=100')}
export const storedKey='aecai-latest-execution-v2';
export function storeRun(data){sessionStorage.setItem(storedKey,JSON.stringify(data))}
export function fromCache(){try{return JSON.parse(sessionStorage.getItem(storedKey)||'null')}catch{return null}}
export async function reportFor(id){
 if(id){try{return await getJson('/api/v1/integrated/report/'+encodeURIComponent(id))}catch(e){throw Error('This run has no full report yet. Older runs may only have summary data.')}}
 const cached=fromCache();if(cached)return cached;const rows=await getRuns();if(rows.length) return reportFor(rows[0].ID);throw Error('No execution found yet. Run a task first.')
}
export async function renderServiceChecks(){const container=$('services');if(!container)return;const names=['Dashboard','Edge','Decision','Cloud','Estimator'];const elements={};for(const name of names){const div=document.createElement('div');div.className='service';div.innerHTML=`<span>${esc(name)}</span><span class="chip">Checking…</span>`;container.appendChild(div);elements[name]=div.lastElementChild}try{const checks=await getJson('/api/v1/integrated/services');for(const name of names){const ok=checks[name]===true;elements[name].className='chip '+(ok?'success':'failed');elements[name].textContent=ok?'ONLINE':'OFFLINE'}}catch{for(const name of names){elements[name].className='chip failed';elements[name].textContent='UNKNOWN'}}}
export function routeExplanation(d){
 if(d.status==='NO_FEASIBLE_ROUTE')return 'No execution location satisfied the active constraints. Review the deadline, network and service availability.';
 if(d.status==='FAILED')return 'The execution failed. See the technical details below for the recorded error.';
 const route=d.selectedRoute;let message=route==='EDGE'?'The workload was processed locally on the Edge device.':route==='CLOUD'?'The workload was sent to the Cloud service for inference.':'Hybrid processing split the workload: the Edge extracted features, the Cloud ran inference, and the Edge finalized the prediction.';
 if(d.fallbackUsed)message+=' The preferred remote route failed, so the system fell back to an available Edge route.';
 else message+=' The Decision Engine chose this feasible route using its scoring rules and estimated costs.';
 if(d.decision?.explanation)message+=' Engine reasoning: '+d.decision.explanation;
 return message;
}
export function routeClass(route){return ({EDGE:'edge',CLOUD:'cloud',HYBRID:'hybrid'})[route]||''}
export const routeList=['EDGE','CLOUD','HYBRID'];
export function estimateRows(estimates={}){return routeList.map(name=>{const e=estimates?.[name]||{};return {name,available:e.estimateAvailable===true,ms:e.estimatedLatencyMs,energy:e.energyEstimateJ,carbon:e.carbonEstimateG,reason:e.reason||'Not available'}})}
export function barCharts(estimates){const entries=estimateRows(estimates);const times=entries.filter(x=>x.available&&Number.isFinite(x.ms)).map(x=>x.ms);const max=Math.max(...times,1);return entries.map(r=>`<div><div class="barhead"><span><span class="chip ${routeClass(r.name)}">${r.name}</span> ${r.available?'Estimated latency':'Unavailable'}</span><b>${r.available?money(r.ms)+' ms':esc(r.reason)}</b></div><div class="bartrack">${r.available?`<div class="barfill ${routeClass(r.name)}" style="width:${Math.max(1,Math.min(100,(r.ms/max)*100))}%"></div>`:''}</div></div>`).join('')}
export function recentTable(rows,tbody){tbody.replaceChildren();if(!rows.length){tbody.innerHTML='<tr><td colspan="6" class="empty">No executions yet — submit your first task.</td></tr>';return;}for(const r of rows){const tr=document.createElement('tr');const id=r.ID;tr.innerHTML=`<td>${esc(new Date(r.CREATED_AT).toLocaleString())}</td><td>${esc(r.TASK_ID)}</td><td><span class="chip ${r.STATUS==='SUCCESS'?'success':'failed'}">${esc(r.STATUS)}</span></td><td><span class="chip ${routeClass(r.EXECUTED_ROUTE)}">${esc(r.EXECUTED_ROUTE)}</span></td><td>${esc(r.PREDICTION)}</td><td><a class="btn" href="execution-result.html?id=${encodeURIComponent(id)}">View report</a></td>`;tbody.appendChild(tr)}}
