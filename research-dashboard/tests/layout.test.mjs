import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=path=>readFileSync(new URL(path,import.meta.url),'utf8');

test('Reference-aligned decision timeline is eight-stage and selectable',()=>{
  const decision=read('../src/pages/Decision.tsx');
  assert.match(decision,/pipeline-rail/);
  assert.match(decision,/timeline-stage/);
  assert.match(decision,/onClick=\{\(\)=>setStage\(i\)\}/);
  assert.match(decision,/stages\.map/);
});

test('Radar visualization is driven by saved scorer components, not fixed examples',()=>{
  const audit=read('../src/components/Audit.tsx');
  assert.match(audit,/Object\.entries\(comp\)\.map/);
  assert.match(audit,/RadarChart/);
  assert.match(audit,/Normalized utility profile/);
});

test('Dark and light themes remain purple and both exist',()=>{
  const style=read('../src/styles.css');
  assert.match(style,/#09090f/i);
  assert.match(style,/#8b5cf6/i);
  assert.match(style,/data-theme='light'/);
  assert.match(style,/prefers-reduced-motion/);
});
