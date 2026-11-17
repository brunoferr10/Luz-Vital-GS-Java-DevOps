// Integration adapter, not a complete Expo application. Use fictitious data.
import * as SecureStore from 'expo-secure-store';
import AsyncStorage from '@react-native-async-storage/async-storage';
const BASE = process.env.EXPO_PUBLIC_API_URL;
const TOKEN = 'luz-vital-jwt';
const QUEUE = 'luz-vital-outbox';
type Pending = { key: string; body: { pacienteId: number; inicio: string; observacao: string } };
export async function request<T>(path: string, method = 'GET', body?: unknown, extra: Record<string,string> = {}): Promise<T> {
  if (!BASE) throw new Error('Configure EXPO_PUBLIC_API_URL');
  const token = await SecureStore.getItemAsync(TOKEN);
  const response = await fetch(BASE + path, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    if (response.status === 401) await SecureStore.deleteItemAsync(TOKEN);
    const error = await response.json().catch(() => ({ mensagem: 'Falha de comunicação' }));
    throw Object.assign(new Error(error.mensagem || 'Falha de comunicação'), { status: response.status });
  }
  return response.status === 204 ? undefined as T : response.json();
}
export async function login(email: string, senha: string) {
  const result = await request<{accessToken: string; papel: string}>('/auth/login', 'POST', {email, senha});
  await SecureStore.setItemAsync(TOKEN, result.accessToken); return result;
}
export const logout = () => SecureStore.deleteItemAsync(TOKEN);
export const register = (nome: string, email: string, senha: string) => request<{id:number}>('/auth/registro','POST',{nome,email,senha});
export const listPatients = (page=0) => request(`/pacientes?page=${page}&size=20`);
export const createPatient = (input: {nome:string;endereco:string;telefone:string;cuidadorId:number}) => request('/pacientes','POST',input);
export const createDevice = (input: {pacienteId:number;modelo:string;tipo:string;potenciaWatts:number;autonomiaMinutos:number}) => request('/aparelhos','POST',input);
export const managerQueue = () => request('/ocorrencias/fila');
export const generatePlan = (id:number) => request(`/pacientes/${id}/plano`,'POST');
export async function cachePlan(id:number) {
  const p = await request<{planoContingencia:string|null}>(`/pacientes/${id}`);
  if (p.planoContingencia) await AsyncStorage.setItem(`plano-${id}`,p.planoContingencia);
  return p.planoContingencia;
}
// Serialize local storage mutations to avoid losing items on repeated taps.
let work: Promise<unknown> = Promise.resolve();
function serial<T>(fn:()=>Promise<T>):Promise<T> { const next=work.then(fn,fn);work=next.catch(()=>undefined);return next; }
export function queueOutage(item:Pending) {
  // Generate a UUID in the UI once (expo-crypto/randomUUID), retain body/key on retries.
  return serial(async()=>{
    const queue:Pending[]=JSON.parse(await AsyncStorage.getItem(QUEUE)||'[]');
    if (!queue.some(x=>x.key===item.key)) queue.push(item);
    await AsyncStorage.setItem(QUEUE,JSON.stringify(queue));
  });
}
export function flushOutages() {
  return serial(async()=>{
    const queue:Pending[]=JSON.parse(await AsyncStorage.getItem(QUEUE)||'[]');
    while(queue.length) {
      const item=queue[0];
      // Any exception preserves the current item; 401/403/409 require UI intervention.
      await request('/ocorrencias','POST',item.body,{'Idempotency-Key':item.key});
      queue.shift();await AsyncStorage.setItem(QUEUE,JSON.stringify(queue));
    }
  });
}
// Protect patient data at rest before real use: AsyncStorage here is unencrypted.
