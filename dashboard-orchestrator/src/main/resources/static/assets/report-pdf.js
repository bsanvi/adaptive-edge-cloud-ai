/* Small, dependency-free, offline PDF exporter. Creates an A4 report page from a canvas. */
import {money,pct,routeExplanation,estimateRows} from './common.js';
export async function downloadPDF(d){
 const canvas=document.createElement('canvas');canvas.width=1240;canvas.height=1754;const c=canvas.getContext('2d');
 c.fillStyle='#ffffff';c.fillRect(0,0,1240,1754);const ink='#1b2d46',muted='#526680',teal='#127d82',pad=88;
 function text(t,x,y,size=26,color=ink,bold=false){c.font=`${bold?'700':'400'} ${size}px Arial, sans-serif`;c.fillStyle=color;c.fillText(String(t??'—'),x,y)}
 function wrap(t,x,y,maxWidth,step=34,size=23,color=ink,maxLines=12){c.font=`400 ${size}px Arial`;let line='',count=0;for(const w of String(t||'—').split(/\s+/)){let attempt=line?line+' '+w:w;if(c.measureText(attempt).width>maxWidth&&line){text(line,x,y,size,color);y+=step;count++;line=w;if(count>=maxLines)break}else line=attempt}if(line&&count<maxLines){text(line,x,y,size,color);y+=step}return y}
 function row(y,left,right){text(left,pad,y,24,muted);text(right,690,y,25,ink,true)}
 c.fillStyle='#142940';c.fillRect(0,0,1240,220);text('EDGE / CLOUD  •  INTELLIGENT COMPUTING',pad,75,20,'#55e2cf',true);text('Execution Analysis Report',pad,137,44,'#ffffff',true);text('Automatically generated from a completed task',pad,181,22,'#c6d4e5');
 text('EXECUTION SUMMARY',pad,284,22,teal,true);c.strokeStyle='#dae3ed';c.beginPath();c.moveTo(pad,302);c.lineTo(1150,302);c.stroke();
 row(351,'Task ID',d.taskId||'—');row(398,'Status',d.status||'—');row(445,'Selected route',d.selectedRoute||'—');row(492,'Actual route',d.executedRoute||'—');row(539,'Prediction',d.prediction||'—');row(586,'Confidence',pct(d.confidence));row(633,'Measured total time',money(d.totalTimeMs)+' ms');row(680,'Fallback used',d.fallbackUsed?'YES':'NO');
 text('WHY THIS LOCATION?',pad,754,22,teal,true);const endExplanation=wrap(routeExplanation(d),pad,803,1050,33,23,ink,6);
 let y=Math.max(965,endExplanation+30);text('ROUTE COMPARISON (PROTOTYPE ESTIMATES)',pad,y,22,teal,true);y+=52;
 text('Route',pad,y,22,muted,true);text('Latency',390,y,22,muted,true);text('Energy',680,y,22,muted,true);text('Carbon (g)',900,y,22,muted,true);y+=22;c.strokeStyle='#cad6e1';c.beginPath();c.moveTo(pad,y);c.lineTo(1140,y);c.stroke();y+=39;
 for(const row of estimateRows(d.estimates)){text(row.name,pad,y,24,ink,true);text(row.available?money(row.ms)+' ms':'Unavailable',390,y,23);text(row.available?money(row.energy)+' J':'—',680,y,23);text(row.available?money(row.carbon,6):'—',900,y,23);y+=60}
 y+=22;text('NETWORK OBSERVATIONS',pad,y,22,teal,true);y+=48;text('Latency: '+money(d.network?.latencyMs)+' ms',pad,y,23);text('Bandwidth: '+money(d.network?.bandwidthMbps)+' Mbps',640,y,23);y+=67;
 text('INTERPRETATION AND LIMITATIONS',pad,y,22,teal,true);y+=46;wrap('Route latency, energy and carbon figures are estimates based on assumed prototype calibration. The total execution time above is measured end-to-end and is not directly comparable to the estimated compute latency.',pad,y,1030,30,20,muted,5);
 c.fillStyle='#edf2f7';c.fillRect(pad,1632,1060,1);text('Research prototype • reference ONNX model is not a validated anomaly detector',pad,1666,18,muted);
 const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',.90));if(!blob)throw Error('Canvas JPEG generation unavailable');
 const img=new Uint8Array(await blob.arrayBuffer());const enc=new TextEncoder();const parts=[],offsets=[0];let length=0;
 const push=v=>{parts.push(v);length+=v.length};const ascii=s=>push(enc.encode(s));ascii('%PDF-1.4\n');
 function obj(i,s){offsets[i]=length;ascii(`${i} 0 obj\n${s}\nendobj\n`)}
 obj(1,'<< /Type /Catalog /Pages 2 0 R >>');obj(2,'<< /Type /Pages /Kids [3 0 R] /Count 1 >>');obj(3,'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>');
 offsets[4]=length;ascii(`4 0 obj\n<< /Type /XObject /Subtype /Image /Width 1240 /Height 1754 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${img.length} >>\nstream\n`);push(img);ascii('\nendstream\nendobj\n');
 const commands='q\n595 0 0 842 0 0 cm\n/Im0 Do\nQ\n';obj(5,`<< /Length ${enc.encode(commands).length} >>\nstream\n${commands}endstream`);
 const xref=length;ascii('xref\n0 6\n0000000000 65535 f \n');for(let i=1;i<=5;i++)ascii(String(offsets[i]).padStart(10,'0')+' 00000 n \n');ascii(`trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`);
 const out=new Blob(parts,{type:'application/pdf'});const url=URL.createObjectURL(out);const a=document.createElement('a');a.href=url;a.download='edgecloud-report-'+String(d.taskId||'task').replace(/[^a-z0-9_-]/gi,'_')+'.pdf';document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),10000);
}
