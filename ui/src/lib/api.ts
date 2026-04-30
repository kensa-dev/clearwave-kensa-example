import type { FeasibilityRequest, FeasibilityResponse, ServiceAddress } from "./types"

export async function checkFeasibility(address: ServiceAddress): Promise<FeasibilityResponse> {
  const body: FeasibilityRequest = {
    address,
    services: ["VOICE", "BROADBAND"],
  }

  const response = await fetch("/api/feasibility", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json() as Promise<FeasibilityResponse>
}
