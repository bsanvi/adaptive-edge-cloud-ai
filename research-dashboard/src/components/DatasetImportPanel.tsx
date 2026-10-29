import {useState} from 'react';
import {Database,FileUp,LockKeyhole} from 'lucide-react';
import {ErrorNotice,Field,Panel,SourceTag} from './UI';
import {operationsApi} from '../lib/operationsApi';

export function DatasetImportPanel({enabled,onImported}:{enabled:boolean;onImported:()=>void}){
  const [key,setKey]=useState('');
  const [datasetId,setDatasetId]=useState('');
  const [name,setName]=useState('');
  const [source,setSource]=useState('');
  const [license,setLicense]=useState('');
  const [split,setSplit]=useState('test');
  const [label,setLabel]=useState('UNLABELED');
  const [file,setFile]=useState<File|null>(null);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [success,setSuccess]=useState('');
  async function upload(){
    setError('');setSuccess('');setBusy(true);
    try{
      if(!file)throw new Error('Choose a real PNG or JPEG image before importing.');
      if(key.length<24)throw new Error('An operations key is required for dataset imports.');
      const result=await operationsApi.importImage({file,datasetId,name,source,license,split,label,key});
      setSuccess(`Imported ${String(result.fileName)} into ${String(result.datasetId)} as ${String(result.label)}.`);
      onImported();setFile(null);
    }catch(e){setError(e instanceof Error?e.message:String(e))}finally{setBusy(false)}
  }
  if(!enabled)return null;
  return <Panel title="Import verified research samples" overline="DATASET MANAGEMENT / AUTHENTICATED WRITE" className="section-gap" action={<SourceTag type="REAL USER UPLOAD"/>}>
    <p className="subtle">Register a real image with a human-supplied class label, data source, and usage license. Uploaded images are validated and stored in the backend dataset registry. Unlabeled uploads remain UNLABELED, not guessed.</p>
    <div className="form-grid section-gap">
      <Field label="Dataset ID"><input className="input" value={datasetId} onChange={e=>setDatasetId(e.target.value)} placeholder="e.g. lab-images-2026"/></Field>
      <Field label="Dataset name"><input className="input" value={name} onChange={e=>setName(e.target.value)} placeholder="Human-readable dataset name"/></Field>
      <Field label="Data source / citation"><input className="input" value={source} onChange={e=>setSource(e.target.value)} placeholder="Original source URL or creator"/></Field>
      <Field label="License / usage conditions"><input className="input" value={license} onChange={e=>setLicense(e.target.value)} placeholder="e.g. own photographs; permission obtained"/></Field>
      <Field label="Dataset split"><select className="select" value={split} onChange={e=>setSplit(e.target.value)}><option value="train">TRAIN</option><option value="validation">VALIDATION</option><option value="test">TEST</option></select></Field>
      <Field label="Actual label"><input className="input" value={label} onChange={e=>setLabel(e.target.value)} placeholder="UNLABELED if no verified ground truth"/></Field>
      <Field label="Image (PNG/JPEG, max 10 MiB)"><input type="file" accept="image/png,image/jpeg" onChange={e=>setFile(e.target.files?.[0]||null)}/></Field>
      <Field label="Operations key"><input className="input" type="password" autoComplete="off" value={key} onChange={e=>setKey(e.target.value)} placeholder="Secret key from local configuration"/></Field>
    </div>
    <div className="page-actions"><button className="button button-primary" disabled={busy||!file} onClick={()=>void upload()}><FileUp size={16}/>{busy?'Importing…':'Import real sample'}</button><span className="tiny muted"><LockKeyhole size={13}/> Original file name is not used on the server.</span></div>
    {error&&<ErrorNotice message={error}/>} {success&&<div className="notice info section-gap"><Database size={16}/>{success}</div>}
  </Panel>
}
