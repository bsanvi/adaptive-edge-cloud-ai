type CsrfResponse={
  headerName:string;
  parameterName:string;
  token:string;
};

let cached:Promise<CsrfResponse>|null=null;

async function csrf():Promise<CsrfResponse>{
  if(!cached){
    cached=fetch('/api/v1/security/csrf',{
      credentials:'same-origin',
      cache:'no-store'
    }).then(async response=>{
      if(!response.ok){
        throw new Error(`Unable to obtain CSRF token (${response.status})`);
      }

      return await response.json() as CsrfResponse;
    }).catch(error=>{
      cached=null;
      throw error;
    });
  }

  return cached;
}

export async function secureRequest(
  init:RequestInit={}
):Promise<RequestInit>{

  const method=(init.method||'GET').toUpperCase();
  const headers=new Headers(init.headers);

  if(!['GET','HEAD','OPTIONS','TRACE'].includes(method)){
    const token=await csrf();
    headers.set(token.headerName,token.token);
  }

  return {
    ...init,
    headers,
    credentials:'same-origin'
  };
}
