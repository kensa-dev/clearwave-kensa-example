export interface ServiceAddress {
  postcode: string
  addressLine1: string
  town: string
  county: string
}

export interface LineProfile {
  type: string
  description: string
  downloadSpeed: number
  uploadSpeed: number
  supplier: string
}

export interface FeasibilityRequest {
  address: ServiceAddress
  services?: string[]
}

export interface FeasibilityResponse {
  address: ServiceAddress
  serviceable: boolean
  profiles: LineProfile[]
}
