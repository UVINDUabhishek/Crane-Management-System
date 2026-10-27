import { z } from 'zod';
import type { CustomerResponse } from './customersApi';
const required = z.string().trim().min(1, 'This field is required.');
export const customerSchema = z.object({
  companyName: required,
  registrationNo: required,
  vatNo: required,
  companyAddress: required,
  billAddress: required,
  contactPerson: required,
  contactNo: z
    .string()
    .trim()
    .regex(
      /^0\d{9}$/,
      'Enter a 10-digit Sri Lankan phone number starting with 0.',
    ),
  customerEmail: z
    .string()
    .trim()
    .refine(
      (value) => !value || z.email().safeParse(value).success,
      'Enter a valid email address.',
    ),
});
export type CustomerValues = z.infer<typeof customerSchema>;
export const customerFields = {
  companyName: 'Company name',
  registrationNo: 'Registration number',
  vatNo: 'VAT number',
  companyAddress: 'Company address',
  billAddress: 'Billing address',
  contactPerson: 'Contact person',
  contactNo: 'Contact number',
  customerEmail: 'Customer email',
} satisfies Record<keyof CustomerValues, string>;
export const fieldNames = Object.keys(
  customerFields,
) as (keyof CustomerValues)[];
export function formDefaults(customer?: CustomerResponse): CustomerValues {
  return Object.fromEntries(
    fieldNames.map((key) => [key, customer?.[key] ?? '']),
  ) as CustomerValues;
}
