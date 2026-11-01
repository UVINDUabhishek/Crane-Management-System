import { createTheme } from '@mui/material/styles';
interface NavigationColors {
  background: string;
  text: string;
  muted: string;
  hover: string;
  selected: string;
  selectedHover: string;
  accent: string;
  border: string;
}
declare module '@mui/material/styles' {
  interface Palette {
    navigation: NavigationColors;
    surface: { muted: string; accent: string };
  }
  interface PaletteOptions {
    navigation?: NavigationColors;
    surface?: { muted: string; accent: string };
  }
}
export function createAppTheme(dark: boolean) {
  const border = dark ? '#304254' : '#e1e8ef';
  return createTheme({
    palette: {
      mode: dark ? 'dark' : 'light',
      primary: { main: dark ? '#60cdbb' : '#087f78' },
      secondary: { main: dark ? '#a7bacf' : '#243e58' },
      success: { main: dark ? '#80caa5' : '#15803d' },
      warning: { main: dark ? '#e8bd70' : '#b45309' },
      background: {
        default: dark ? '#101a26' : '#f4f7fa',
        paper: dark ? '#182635' : '#fff',
      },
      text: {
        primary: dark ? '#edf3f8' : '#17324d',
        secondary: dark ? '#a6b7c7' : '#5e6e83',
      },
      divider: border,
      error: { main: dark ? '#f09898' : '#b91c1c' },
      navigation: {
        background: dark ? '#0c1724' : '#102d46',
        text: '#edf3f8',
        muted: '#a6b7c7',
        hover: '#203f55',
        selected: '#20515a',
        selectedHover: '#28616a',
        accent: '#8ce0d0',
        border: '#496476',
      },
      surface: {
        muted: dark ? '#152331' : '#f8fafc',
        accent: dark ? '#20443f' : '#e9f5f1',
      },
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
      MuiCssBaseline: {
        styleOverrides: {
          'a:focus-visible, [tabindex="0"]:focus-visible': {
            outline: `2px solid ${dark ? '#60cdbb' : '#087f78'}`,
            outlineOffset: 3,
          },
          '@media (prefers-reduced-motion: reduce)': {
            '*, *::before, *::after': {
              animation: 'none !important',
              transition: 'none !important',
              scrollBehavior: 'auto !important',
            },
          },
        },
      },
      MuiButtonBase: {
        styleOverrides: {
          root: {
            '&.Mui-focusVisible': {
              outline: `2px solid ${dark ? '#60cdbb' : '#087f78'}`,
              outlineOffset: 3,
            },
          },
        },
      },
      MuiCheckbox: { styleOverrides: { root: { padding: 11 } } },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: {
            minHeight: 44,
            borderRadius: 8,
            padding: '8px 16px',
            whiteSpace: 'nowrap',
          },
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
          root: { borderRadius: 6, fontWeight: 600, fontSize: '.75rem' },
          clickable: {
            minHeight: 32,
            '@media (pointer: coarse)': { minHeight: 44 },
          },
        },
      },
      MuiTextField: { defaultProps: { size: 'small' } },
      MuiInputBase: {
        styleOverrides: {
          input: ({ theme }) => ({
            '&::placeholder': {
              color: theme.palette.text.secondary,
              opacity: 1,
            },
          }),
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: { borderRadius: 8, fontSize: '.875rem' },
          notchedOutline: { borderColor: dark ? '#7c8fa3' : '#7b8b9d' },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: { borderColor: border, padding: '13px 16px' },
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
      MuiIconButton: {
        styleOverrides: {
          root: { borderRadius: 8, minWidth: 44, minHeight: 44 },
        },
      },
    },
  });
}
