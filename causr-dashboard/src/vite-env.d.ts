/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BFF_API_BASE?: string;
  readonly VITE_PROCESSOR_API_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
