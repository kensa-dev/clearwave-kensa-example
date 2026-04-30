import { CheckCircle2, XCircle, WifiOff } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import type { FeasibilityResponse, LineProfile } from "@/lib/types"

interface ResultsPanelProps {
  response: FeasibilityResponse | null
  hasError: boolean
}

type TechType = "FTTP" | "FTTC" | "ADSL" | string

function techBadgeClass(type: TechType): string {
  switch (type.toUpperCase()) {
    case "FTTP":
      return "cw-badge-fttp"
    case "FTTC":
      return "cw-badge-fttc"
    case "ADSL":
      return "cw-badge-adsl"
    default:
      return "cw-badge-default"
  }
}

function ProfileCard({ profile }: { profile: LineProfile }) {
  return (
    <Card data-testid="profile-card" className="cw-profile-card">
      <CardContent className="cw-profile-content">
        <div className="cw-profile-speeds">
          <div className="cw-speed-block">
            <span className="cw-speed-value">{profile.downloadSpeed}</span>
            <span className="cw-speed-unit">Mbps</span>
            <span className="cw-speed-label">down</span>
          </div>
          <div className="cw-speed-divider" aria-hidden="true" />
          <div className="cw-speed-block cw-speed-upload">
            <span className="cw-speed-value cw-speed-value--upload">{profile.uploadSpeed}</span>
            <span className="cw-speed-unit">Mbps</span>
            <span className="cw-speed-label">up</span>
          </div>
        </div>
        <div className="cw-profile-meta">
          <div className="cw-profile-desc-row">
            <span className="cw-profile-description">{profile.description}</span>
            <Badge className={`cw-tech-badge ${techBadgeClass(profile.type)}`}>
              {profile.type}
            </Badge>
          </div>
          <p className="cw-profile-supplier">via {profile.supplier}</p>
        </div>
      </CardContent>
    </Card>
  )
}

export function ResultsPanel({ response, hasError }: ResultsPanelProps) {
  if (hasError) {
    return (
      <div className="cw-results">
        <Alert variant="destructive" className="cw-alert">
          <WifiOff className="size-4" />
          <AlertTitle>Connection error</AlertTitle>
          <AlertDescription>
            Couldn't check availability — please try again.
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  if (!response) return null

  const isServiceable = response.serviceable && response.profiles.length > 0

  if (!isServiceable) {
    return (
      <div className="cw-results">
        <Alert data-testid="results-not-serviceable" variant="destructive" className="cw-alert">
          <XCircle className="size-4" />
          <AlertTitle>No service available</AlertTitle>
          <AlertDescription>
            No service available at this address. We're working on bringing Clearwave to your area.
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="cw-results">
      <Alert data-testid="results-success" className="cw-alert cw-alert--success">
        <CheckCircle2 className="size-4" />
        <AlertTitle>Service available at this address</AlertTitle>
        <AlertDescription>
          {response.profiles.length} plan{response.profiles.length !== 1 ? "s" : ""} available.
        </AlertDescription>
      </Alert>
      <div className="cw-profiles-list">
        {response.profiles.map((profile, i) => (
          <ProfileCard key={i} profile={profile} />
        ))}
      </div>
    </div>
  )
}
