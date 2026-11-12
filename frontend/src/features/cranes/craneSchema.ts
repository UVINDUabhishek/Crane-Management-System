import { z } from 'zod';
import type { CraneResponse } from './cranesApi';
const required = z.string().trim().min(1, 'This field is required.');
export const craneSchema = z.object({
  type: required,
  capacity: z
    .number({ error: 'Enter a valid capacity.' })
    .positive('Capacity must be greater than 0.'),
  manufacturer: required,
  serialNumber: required,
  year: z
    .number({ error: 'Enter a valid year.' })
    .int('Year must be a whole number.')
    .positive('Year must be greater than 0.'),
  location: required,
});
export type CraneValues = z.infer<typeof craneSchema>;
export const craneFields = {
  type: 'Type',
  capacity: 'Capacity',
  manufacturer: 'Manufacturer',
  serialNumber: 'Serial number',
  year: 'Year',
  location: 'Location',
};
export const fieldNames = Object.keys(craneFields) as (keyof CraneValues)[];
export function formDefaults(c?: CraneResponse): CraneValues {
  return {
    type: c?.type ?? '',
    capacity: c?.capacity ?? NaN,
    manufacturer: c?.manufacturer ?? '',
    serialNumber: c?.serialNumber ?? '',
    year: c?.year ?? NaN,
    location: c?.location ?? '',
  };
}
