import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import path from 'node:path';
const src=path.resolve(import.meta.dirname,'../src');
const read=p=>fs.readFileSync(path.join(src,p),'utf8');
test('Eight routed pages exist',()=>{const app=read('App.tsx');for(const key of ['/decision','/results','/lab','/datasets','/analytics','/architecture','/settings']) assert.ok(app.includes(key),`missing ${key}`)});
test('Dashboard consumes live backend contracts instead of fake scores',()=>{const api=read('lib/api.ts');for(const uri of ['/telemetry','/services','/execute','/history','/report/','/research/experiments/evaluate']) assert.ok(api.includes(uri));assert.ok(!api.includes('Math.random()'),'API must not invent metrics');});
test('Privacy remains a hard constraint in backend workflow',()=>{const source=fs.readFileSync(path.resolve(import.meta.dirname,'../../dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/research/ResearchExperimentController.java'),'utf8');assert.ok(source.includes('"privacy_required",s.privacyRequired()'));assert.ok(!source.includes('disablePrivacy'));});
test('Saved audit is displayed inside decision and results',()=>{assert.ok(read('pages/Decision.tsx').includes('<Audit '));assert.ok(read('pages/Results.tsx').includes('<Audit '));});
test('Source indicators distinguish measured, estimated and simulated',()=>{const ui=read('components/UI.tsx');assert.ok(ui.includes('MEASURED'));assert.ok(ui.includes('ESTIMAT'));assert.ok(ui.includes('SIMULAT'));});

test('Unavailable estimator routes are not marked feasible',()=>{const lab=read('pages/Lab.tsx');assert.ok(lab.includes('estimateAvailable===true'), 'Must check estimateAvailable rather than object truthiness');});
test('Benchmark provenance requires Dataset Explorer sample',()=>{const over=read('pages/Overview.tsx');assert.ok(over.includes("task.source==='BENCHMARK DATASET'&&<option"), 'Do not enable a fabricated benchmark source in manual entry');});
