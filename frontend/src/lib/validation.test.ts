import { describe, expect, it } from 'vitest'
import { validateApplication, validateCredentials } from './validation'
import type { ApplicationFormValues } from './validation'

const valid: ApplicationFormValues = {
  company: 'Acme',
  position: 'Utvikler',
  status: 'WISHLIST',
  deadline: '',
  appliedDate: '',
  jobUrl: '',
  notes: '',
}

describe('validateApplication', () => {
  it('accepts a minimal application', () => {
    expect(validateApplication(valid)).toEqual({})
  })

  it('requires company and position, ignoring whitespace', () => {
    const errors = validateApplication({ ...valid, company: '  ', position: '' })
    expect(errors.company).toBe('Selskap må fylles ut')
    expect(errors.position).toBe('Stilling må fylles ut')
  })

  it('mirrors the backend length limits', () => {
    expect(validateApplication({ ...valid, company: 'x'.repeat(256) }).company).toMatch(
      '255',
    )
  })

  it('only accepts http(s) job links', () => {
    expect(validateApplication({ ...valid, jobUrl: 'https://acme.no/jobb' })).toEqual({})
    expect(
      validateApplication({ ...valid, jobUrl: 'javascript:alert(1)' }).jobUrl,
    ).toBeDefined()
    expect(validateApplication({ ...valid, jobUrl: 'acme.no' }).jobUrl).toBeDefined()
  })
})

describe('validateCredentials', () => {
  it('enforces the 10-72 character password rule only when asked', () => {
    expect(validateCredentials('a@b.no', 'kort', { checkPasswordLength: false })).toEqual(
      {},
    )
    expect(
      validateCredentials('a@b.no', 'kort', { checkPasswordLength: true }).password,
    ).toBeDefined()
    expect(
      validateCredentials('a@b.no', 'x'.repeat(73), { checkPasswordLength: true })
        .password,
    ).toBeDefined()
  })
})
