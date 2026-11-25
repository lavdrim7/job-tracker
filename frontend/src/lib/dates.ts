/** Formats a yyyy-MM-dd string without going through Date (avoids timezone shifts). */
export function formatDate(value: string | null): string {
  if (!value) return '–'
  const [year, month, day] = value.split('-')
  return `${day}.${month}.${year}`
}
