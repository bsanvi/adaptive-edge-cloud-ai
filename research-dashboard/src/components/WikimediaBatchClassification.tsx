import {useEffect, useRef, useState} from 'react';
import {Download, Play, ShieldCheck} from 'lucide-react';
import {api, asText, csv, fmt, num, obj, safeDownload} from '../lib/api';
import type {DatasetSample} from '../lib/api';
import {DataTable, ErrorNotice, SourceTag} from './UI';

type ResultRow = {
  fileName: string;
  datasetLabel: string;
  predictedLabel: string;
  confidence: number | null;
  modelTimeMs: number | null;
  taskId: string;
  status: 'SUCCESS' | 'FAILED';
  error: string;
};

const MAX_BATCH = 10;
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

// Runs on the user's own Dashboard domain. No browser-supplied filesystem paths,
// third-party endpoints, or hard-coded example predictions are used.
export function WikimediaBatchClassification({datasetId, samples}: {
  datasetId: string;
  samples: DatasetSample[];
}) {
  const [rows, setRows] = useState<ResultRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [completed, setCompleted] = useState(0);
  const [message, setMessage] = useState('');
  const runId = useRef(0);
  useEffect(() => {
    runId.current += 1;
    setRows([]);
    setCompleted(0);
    setBusy(false);
    setMessage('');
  }, [datasetId]);

  const targets = samples.filter(s => s.label.toLowerCase() !== 'dog').slice(0, MAX_BATCH);

  async function classifyAll() {
    if (busy || targets.length === 0) return;
    const currentRun = ++runId.current;
    setRows([]);
    setCompleted(0);
    setMessage('');
    setBusy(true);
    try {
      for (let i = 0; i < targets.length; i++) {
        if (currentRun !== runId.current) break;
        const sample = targets[i];
        const base: ResultRow = {
          fileName: sample.fileName, datasetLabel: sample.label,
          predictedLabel: '—', confidence: null, modelTimeMs: null,
          taskId: '', status: 'FAILED', error: ''
        };
        try {
          const url = new URL(sample.previewUrl, window.location.origin);
          if (url.origin !== window.location.origin ||
              !url.pathname.startsWith('/api/v1/research/datasets/')) {
            throw new Error('Unexpected sample source; only same-origin dataset images are permitted');
          }
          const downloaded = await fetch(url.toString(), {cache: 'no-store'});
          if (!downloaded.ok) throw new Error(`Dataset download failed (HTTP ${downloaded.status})`);
          const blob = await downloaded.blob();
          if (blob.size < 1 || blob.size > MAX_IMAGE_BYTES ||
              !['image/jpeg', 'image/png'].includes(blob.type)) {
            throw new Error('Dataset sample must be a JPEG or PNG of at most 10 MiB');
          }
          const taskId = `batch-${Date.now()}-${i}`;
          const file = new File([blob], sample.fileName, {type: blob.type});
          // Reuses the existing Dashboard -> Edge image upload/profiling route.
          const profile = await api.uploadImage(file, {
            taskId, deadlineMs: 10000, privacyLevel: 'NORMAL', priorityProfile: 'BALANCED'
          });
          // The Dashboard backend records a successful classifier run in H2.
          const result = obj(await api.classifyImage(profile.ref));
          if (result.status !== 'success') throw new Error(`Classifier returned ${asText(result.status)}`);
          const output: ResultRow = {
            ...base, taskId: asText(result.taskId) === '—' ? taskId : String(result.taskId),
            status: 'SUCCESS', predictedLabel: asText(result.predicted_label),
            confidence: num(result.confidence), modelTimeMs: num(result.execution_time_ms)
          };
          if (currentRun === runId.current) setRows(previous => [...previous, output]);
        } catch (error) {
          if (currentRun === runId.current) {
            setRows(previous => [...previous, {
              ...base, error: error instanceof Error ? error.message : String(error)
            }]);
          }
        }
        if (currentRun === runId.current) setCompleted(i + 1);
      }
      if (currentRun === runId.current) setMessage('Batch completed. Each successful image inference is separately saved in H2 execution history.');
    } finally {
      if (currentRun === runId.current) setBusy(false);
    }
  }

  function exportResults() {
    safeDownload(csv(rows.map(r => ({
      dataset: datasetId,
      fileName: r.fileName,
      humanLabel: r.datasetLabel,
      predictedLabel: r.predictedLabel,
      confidence: r.confidence,
      modelExecutionTimeMs: r.modelTimeMs,
      taskId: r.taskId,
      status: r.status,
      error: r.error
    }))), `image-classification-${datasetId}.csv`, 'text/csv');
  }

  return <div className="section-gap">
    <div className="section-heading">AI classification results <SourceTag type="LIVE EDGE INFERENCE"/></div>
    <p className="tiny muted">
      Process each registered image using Person 1's actual SqueezeNet model. This is EDGE-only
      classification, not an adaptive EDGE/CLOUD/HYBRID routing test. Dataset labels are
      human-provided; predictions and timings come from the model. Dog samples are excluded.
    </p>
    <div className="page-actions section-gap">
      <button className="button button-primary button-sm" disabled={busy || targets.length === 0}
        onClick={() => void classifyAll()}>
        <Play size={14}/>{busy ? `Classifying ${completed}/${targets.length}…` : `Run AI Classification on All ${targets.length} Images`}
      </button>
      <button className="button button-secondary button-sm" disabled={busy || rows.length === 0}
        onClick={exportResults}><Download size={14}/>Export CSV</button>
      <span className="tiny muted"><ShieldCheck size={13}/>Existing dataset remains unchanged</span>
    </div>
    {samples.length > MAX_BATCH && <div className="notice info">
      This panel processes at most {MAX_BATCH} samples per run to avoid overloading the local Edge Service.
    </div>}
    <DataTable headers={['Image', 'Dataset label', 'SqueezeNet prediction', 'Confidence', 'Model time', 'Status']} rows={
      rows.map(r => [
        r.fileName, r.datasetLabel, r.predictedLabel,
        r.confidence === null ? '—' : `${fmt(r.confidence * 100, 2)}%`,
        r.modelTimeMs === null ? '—' : `${fmt(r.modelTimeMs, 3)} ms`,
        r.status === 'SUCCESS' ? 'SUCCESS' : `FAILED: ${r.error}`
      ])
    }/>
    {message && <div className="notice info section-gap">{message}</div>}
    {rows.some(r => r.status === 'FAILED') && <ErrorNotice message="Some images failed. Review their error rows. Successful predictions were still saved."/>}
    <p className="tiny muted section-gap">Inference time is reported by SqueezeNet; it excludes the browser upload, network request, and dashboard rendering time.</p>
  </div>;
}
