import React from 'react';import{createRoot}from'react-dom/client';import{QueryClient,QueryClientProvider}from'@tanstack/react-query';import{HashRouter}from'react-router-dom';import{ProjectProvider}from'./lib/store';import{App}from'./App';import'./styles.css';
const client=new QueryClient({defaultOptions:{queries:{retry:1,refetchOnWindowFocus:false}}});
createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={client}><ProjectProvider><HashRouter><App/></HashRouter></ProjectProvider></QueryClientProvider></React.StrictMode>);
