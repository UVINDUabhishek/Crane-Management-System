import { createTheme } from '@mui/material/styles';
export function createAppTheme(dark: boolean) {
  const border = dark ? '#304251' : '#e1e7ed';
  return createTheme({
    palette: {
      mode: dark ? 'dark' : 'light',
      primary: { main: dark ? '#70cfc1' : '#12675f' },
      secondary: { main: dark ? '#a7bacf' : '#243e58' },
      success: { main: dark ? '#80caa5' : '#28764b' },
      warning: { main: dark ? '#e8bd70' : '#946018' },
      background: {
        default: dark ? '#101b25' : '#f3f6f9',
        paper: dark ? '#192732' : '#fff',
      },
      text: {
        primary: dark ? '#edf3f7' : '#192e42',
        secondary: dark ? '#acbac6' : '#5b6e7f',
      },
      divider: border,
    },
    spacing: 8,
    shape: { borderRadius: 12 },
    typography: {
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
      h4: { fontSize: '1.8rem', fontWeight: 750, letterSpacing: '-.04em' },
      h5: { fontWeight: 700, letterSpacing: '-.025em' },
      h6: { fontSize: '1rem', fontWeight: 700 },
      body1: { fontSize: '.94rem' },
      body2: { fontSize: '.84rem', lineHeight: 1.6 },
      overline: { fontSize: '.66rem', fontWeight: 750, letterSpacing: '.12em' },
      button: { fontWeight: 650, textTransform: 'none' },
    },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: { borderRadius: 8, padding: '8px 16px', whiteSpace: 'nowrap' },
        },
      },
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: { root: { backgroundImage: 'none' } },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            border: `1px solid ${border}`,
            boxShadow: dark ? 'none' : '0 3px 12px rgba(27,46,65,.025)',
          },
        },
      },
      MuiChip: {
        defaultProps: { size: 'small' },
        styleOverrides: {
          root: { borderRadius: 6, fontWeight: 600, fontSize: '.72rem' },
        },
      },
      MuiTextField: { defaultProps: { size: 'small' } },
      MuiOutlinedInput: {
        styleOverrides: { root: { borderRadius: 8, fontSize: '.875rem' } },
      },
      MuiTableCell: {
        styleOverrides: {
          root: { borderColor: border, padding: '16px 20px' },
          head: {
            backgroundColor: dark ? '#1d303e' : '#f8fafc',
            color: dark ? '#b5c7d6' : '#566c7f',
            fontSize: '.68rem',
            fontWeight: 750,
            textTransform: 'uppercase',
            letterSpacing: '.07em',
          },
        },
      },
      MuiTableRow: {
        styleOverrides: { root: { '&:last-child td': { borderBottom: 0 } } },
      },
      MuiDrawer: { styleOverrides: { paper: { backgroundImage: 'none' } } },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            boxShadow: 'none',
            borderBottom: `1px solid ${border}`,
          },
        },
      },
      MuiTooltip: { defaultProps: { arrow: true } },
      MuiIconButton: { styleOverrides: { root: { borderRadius: 8 } } },
    },
  });
}
