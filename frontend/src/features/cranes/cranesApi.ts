import { api } from '../../shared/api';
import type { CraneValues } from './craneSchema';
export interface CraneResponse {
  id: number;
  craneNumber: string;
  type: string;
  capacity: number;
  manufacturer: string | null;
  serialNumber: string;
  year: number;
  status: 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';
  active: boolean;
  location: string | null;
}
export const craneKeys = {
  all: ['cranes'] as const,
  detail: (id: number) => ['cranes', id] as const,
};
export const cranesApi = {
  list: async (signal?: AbortSignal) =>
    (await api.get<CraneResponse[]>('/cranes', { signal })).data,
  detail: async (id: number, signal?: AbortSignal) =>
    (await api.get<CraneResponse>(`/cranes/${id}`, { signal })).data,
  create: async (values: CraneValues) =>
    (await api.post<CraneResponse>('/cranes', values)).data,
  update: async (id: number, values: CraneValues) =>
    (await api.put<CraneResponse>(`/cranes/${id}`, values)).data,
  activation: async ({ id, active }: { id: number; active: boolean }) =>
    (await api.patch<CraneResponse>(`/cranes/${id}/activation`, { active }))
      .data,
};
