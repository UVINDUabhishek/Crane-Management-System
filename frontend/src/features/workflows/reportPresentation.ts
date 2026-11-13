// Presentation types mirror the existing Java report DTOs. No response fields are removed.
export interface JobReport {
  jobCardNumber: string | null;
  customerName: string | null;
  craneNumber: string | null;
  salesPerson: string | null;
  operatorName: string | null;
  jobDate: string | null;
  startDate: string | null;
  endDate: string | null;
  location: string | null;
  totalWorkedHours: number | null;
  totalBillableHours: number | null;
  status: string | null;
}
export interface InvoiceReport {
  invoiceNumber: string | null;
  customerName: string | null;
  craneNumber: string | null;
  jobCardNumber: string | null;
  invoiceDate: string | null;
  qty: number | null;
  rate: number | null;
  amount: number | null;
  mobilization: number | null;
  description: string | null;
  status: string | null;
}
export interface CraneUtilization {
  craneNumber: string | null;
  craneType: string | null;
  totalJobs: number | null;
  totalWorkedHours: number | null;
}
export interface RevenueSummary {
  totalInvoices: number | null;
  totalAmount: number | null;
  totalMobilization: number | null;
  totalRevenue: number | null;
}
export interface MonthlyRevenue {
  year: number | null;
  month: number | null;
  monthName: string | null;
  totalAmount: number | null;
  totalMobilization: number | null;
  totalRevenue: number | null;
  totalInvoices: number | null;
}
export interface CustomerRevenue {
  customerId: number | null;
  customerName: string | null;
  totalInvoices: number | null;
  totalAmount: number | null;
  totalMobilization: number | null;
  totalRevenue: number | null;
}
export interface CraneRevenue {
  craneId: number | null;
  craneNumber: string | null;
  craneType: string | null;
  totalInvoices: number | null;
  totalAmount: number | null;
  totalMobilization: number | null;
  totalRevenue: number | null;
}
export interface MaintenanceReport {
  maintenanceNumber: string | null;
  craneNumber: string | null;
  craneType: string | null;
  maintenanceDate: string | null;
  startDate: string | null;
  endDate: string | null;
  repairType: string | null;
  maintenancePerson: string | null;
  status: string | null;
}
export interface MaintenanceSummary {
  craneId: number | null;
  craneNumber: string | null;
  craneType: string | null;
  totalMaintenanceRecords: number | null;
  totalMaintenanceDays: number | null;
}
export interface MaintenanceStatus {
  status: string | null;
  total: number | null;
}
export type ReportRow = Partial<
  JobReport &
    InvoiceReport &
    CraneUtilization &
    RevenueSummary &
    MonthlyRevenue &
    CustomerRevenue &
    CraneRevenue &
    MaintenanceReport &
    MaintenanceSummary &
    MaintenanceStatus
>;
export type ReportColumn = {
  key: keyof ReportRow;
  label: string;
  kind: 'text' | 'number' | 'money' | 'date' | 'status';
};
function columns<T>(
  entries: {
    key: keyof T & keyof ReportRow;
    label: string;
    kind: ReportColumn['kind'];
  }[],
): ReportColumn[] {
  return entries;
}
const jobs = columns<JobReport>([
  { key: 'jobCardNumber', label: 'Job Card Number', kind: 'text' },
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'customerName', label: 'Customer Name', kind: 'text' },
  { key: 'location', label: 'Location', kind: 'text' },
  { key: 'startDate', label: 'Start Date', kind: 'date' },
  { key: 'endDate', label: 'End Date', kind: 'date' },
  { key: 'salesPerson', label: 'Sales Person', kind: 'text' },
  { key: 'operatorName', label: 'Operator', kind: 'text' },
  { key: 'status', label: 'Status', kind: 'status' },
  { key: 'totalWorkedHours', label: 'Total Worked Hours', kind: 'number' },
]);
const invoices = columns<InvoiceReport>([
  { key: 'invoiceDate', label: 'Invoice Date', kind: 'date' },
  { key: 'invoiceNumber', label: 'Invoice Number', kind: 'text' },
  { key: 'jobCardNumber', label: 'Job Card Number', kind: 'text' },
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'customerName', label: 'Customer Name', kind: 'text' },
  { key: 'description', label: 'Description', kind: 'text' },
  { key: 'amount', label: 'Amount', kind: 'money' },
]);
const crane = columns<CraneUtilization>([
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'craneType', label: 'Crane Type', kind: 'text' },
  { key: 'totalJobs', label: 'Total Jobs', kind: 'number' },
  { key: 'totalWorkedHours', label: 'Total Worked Hours', kind: 'number' },
]);
const summary = columns<RevenueSummary>([
  { key: 'totalInvoices', label: 'Total Invoices', kind: 'number' },
  { key: 'totalAmount', label: 'Total Amount', kind: 'money' },
  { key: 'totalMobilization', label: 'Total Mobilization', kind: 'money' },
  { key: 'totalRevenue', label: 'Total Revenue', kind: 'money' },
]);
const monthly = columns<MonthlyRevenue>([
  { key: 'year', label: 'Year', kind: 'text' },
  { key: 'month', label: 'Month Number', kind: 'text' },
  { key: 'monthName', label: 'Month', kind: 'text' },
  { key: 'totalInvoices', label: 'Total Invoices', kind: 'number' },
  { key: 'totalAmount', label: 'Total Amount', kind: 'money' },
  { key: 'totalMobilization', label: 'Total Mobilization', kind: 'money' },
  { key: 'totalRevenue', label: 'Total Revenue', kind: 'money' },
]);
const customerRevenue = columns<CustomerRevenue>([
  { key: 'customerId', label: 'Customer ID', kind: 'text' },
  { key: 'customerName', label: 'Customer Name', kind: 'text' },
  { key: 'totalInvoices', label: 'Total Invoices', kind: 'number' },
  { key: 'totalAmount', label: 'Total Amount', kind: 'money' },
  { key: 'totalMobilization', label: 'Total Mobilization', kind: 'money' },
  { key: 'totalRevenue', label: 'Total Revenue', kind: 'money' },
]);
const craneRevenue = columns<CraneRevenue>([
  { key: 'craneId', label: 'Crane ID', kind: 'text' },
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'craneType', label: 'Crane Type', kind: 'text' },
  { key: 'totalInvoices', label: 'Total Invoices', kind: 'number' },
  { key: 'totalAmount', label: 'Total Amount', kind: 'money' },
  { key: 'totalMobilization', label: 'Total Mobilization', kind: 'money' },
  { key: 'totalRevenue', label: 'Total Revenue', kind: 'money' },
]);
const maintenance = columns<MaintenanceReport>([
  { key: 'maintenanceNumber', label: 'Maintenance Number', kind: 'text' },
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'craneType', label: 'Crane Type', kind: 'text' },
  { key: 'repairType', label: 'Repair Type', kind: 'text' },
  { key: 'startDate', label: 'Start Date', kind: 'date' },
  { key: 'endDate', label: 'End Date', kind: 'date' },
  { key: 'maintenancePerson', label: 'Maintenance Person', kind: 'text' },
  { key: 'status', label: 'Status', kind: 'status' },
]);
const maintenanceSummary = columns<MaintenanceSummary>([
  { key: 'craneNumber', label: 'Crane Number', kind: 'text' },
  { key: 'craneType', label: 'Crane Type', kind: 'text' },
  {
    key: 'totalMaintenanceRecords',
    label: 'Total Maintenance Records',
    kind: 'number',
  },
  {
    key: 'totalMaintenanceDays',
    label: 'Total Maintenance Days',
    kind: 'number',
  },
]);
const maintenanceStatus = columns<MaintenanceStatus>([
  { key: 'status', label: 'Status', kind: 'status' },
  { key: 'total', label: 'Total', kind: 'number' },
]);
export const reportColumns = {
  jobs,
  jobsCustomer: jobs.filter((c) => c.key !== 'customerName'),
  jobsCrane: jobs.filter((c) => c.key !== 'craneNumber'),
  jobsStatus: jobs.filter((c) => c.key !== 'status'),
  jobsSales: jobs.filter((c) => c.key !== 'salesPerson'),
  invoices,
  invoicesCustomer: invoices.filter((c) => c.key !== 'customerName'),
  crane,
  summary,
  monthly,
  customerRevenue,
  craneRevenue,
  maintenance,
  maintenanceSummary,
  maintenanceStatus,
};
export type ReportView = keyof typeof reportColumns;
