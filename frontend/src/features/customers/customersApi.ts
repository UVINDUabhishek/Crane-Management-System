import { api } from '../../shared/api';
import type { CustomerValues } from './customerSchema';
export interface CustomerResponse {
  id: number;
  active: boolean;
  companyName: string;
  registrationNo: string;
  vatNo: string;
  companyAddress: string;
  billAddress: string | null;
  contactPerson: string | null;
  contactNo: string | null;
  customerEmail: string | null;
}
export const customerKeys = {
  all: ['customers'] as const,
  detail: (id: number) => ['customers', id] as const,
};
export const customersApi = {
  activation: async ({ id, active }: { id: number; active: boolean }) =>
    (
      await api.patch<CustomerResponse>(`/customers/${id}/activation`, {
        active,
      })
    ).data,
  list: async (signal?: AbortSignal) =>
    (await api.get<CustomerResponse[]>('/customers', { signal })).data,
  detail: async (id: number, signal?: AbortSignal) =>
    (await api.get<CustomerResponse>(`/customers/${id}`, { signal })).data,
  create: async (values: CustomerValues) =>
    (await api.post<CustomerResponse>('/customers', values)).data,
  update: async (id: number, values: CustomerValues) =>
    (await api.put<CustomerResponse>(`/customers/${id}`, values)).data,
  deactivate: async (id: number) => {
    await api.delete(`/customers/${id}`);
  },
};
