import {cpSync,mkdirSync} from 'node:fs';
const destination=new URL('../../dashboard-orchestrator/src/main/resources/static/research/',import.meta.url);
mkdirSync(destination,{recursive:true});
cpSync(new URL('../dist/',import.meta.url),destination,{recursive:true});
console.log('Dashboard bundle copied to Spring Boot /research/index.html');
