import { Box, Divider, Stack, Typography } from '@mui/material';
import { formatValue, Value } from './presentation';
import type { Row } from './WorkflowPage';

/** Read-only invoice document. Amount is the server total, including mobilization. */
export function InvoiceDetails({ invoice }: { invoice: Row }) {
  const references = [
    ['customer', 'Customer'],
    ['crane', 'Crane'],
    ['jobCard', 'Job card'],
    ['quotation', 'Quotation'],
  ];
  return (
    <Stack spacing={3} sx={{ overflowWrap: 'anywhere' }}>
      <Box component="section" aria-label="Invoice identity">
        <Typography variant="overline" color="text.secondary">
          Invoice
        </Typography>
        <Typography component="h2" variant="h5">
          {formatValue('invoiceNumber', invoice.invoiceNumber)}
        </Typography>
        <Box
          component="dl"
          sx={{ m: 0, mt: 2, display: 'flex', flexWrap: 'wrap', gap: 3 }}
        >
          {['invoiceDate', 'status', 'version'].map((key) => (
            <Box key={key}>
              <Typography
                component="dt"
                variant="caption"
                color="text.secondary"
              >
                {key === 'invoiceDate'
                  ? 'Invoice date'
                  : key === 'status'
                    ? 'Status'
                    : 'Version'}
              </Typography>
              <Box component="dd" sx={{ m: 0, mt: 0.5 }}>
                <Value field={key} row={invoice} />
              </Box>
            </Box>
          ))}
        </Box>
      </Box>
      <Divider />
      <Box
        component="dl"
        sx={{
          m: 0,
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,minmax(0,1fr))' },
          gap: 2,
        }}
      >
        {references.map(([key, title]) => (
          <Box key={key}>
            <Typography component="dt" variant="caption" color="text.secondary">
              {title}
            </Typography>
            <Typography component="dd" sx={{ m: 0 }}>
              {formatValue(key, invoice[key])}
            </Typography>
          </Box>
        ))}
      </Box>
      <Box
        component="section"
        aria-label="Invoice charges"
        sx={{
          border: 1,
          borderColor: 'divider',
          borderRadius: 1,
          overflow: 'hidden',
        }}
      >
        <Box
          component="dl"
          sx={{
            m: 0,
            p: 2,
            display: 'grid',
            gridTemplateColumns: 'repeat(2,minmax(0,1fr))',
            gap: 2,
          }}
        >
          {['qty', 'rate', 'mobilization'].map((key) => (
            <Box key={key}>
              <Typography
                component="dt"
                variant="caption"
                color="text.secondary"
              >
                {key === 'qty'
                  ? 'Quantity'
                  : key === 'rate'
                    ? 'Rate'
                    : 'Mobilization'}
              </Typography>
              <Typography
                component="dd"
                sx={{ m: 0, fontVariantNumeric: 'tabular-nums' }}
              >
                {formatValue(key, invoice[key])}
              </Typography>
            </Box>
          ))}
        </Box>
        <Box
          sx={{
            p: 2,
            bgcolor: 'surface.muted',
            borderTop: 1,
            borderColor: 'divider',
          }}
        >
          <Typography variant="body2" color="text.secondary">
            Total amount
          </Typography>
          <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {formatValue('amount', invoice.amount)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Includes mobilization
          </Typography>
        </Box>
      </Box>
      <Box component="section" aria-label="Invoice description">
        <Typography component="h3" variant="subtitle2">
          Description
        </Typography>
        <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
          {formatValue('description', invoice.description)}
        </Typography>
      </Box>
    </Stack>
  );
}
