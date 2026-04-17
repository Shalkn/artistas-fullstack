import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Combina classes condicionais (`clsx`) e resolve conflitos Tailwind (`tailwind-merge`).
 *
 * @param inputs valores aceitos por `clsx` (strings, objetos, arrays)
 * @returns string única segura para `className`
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
