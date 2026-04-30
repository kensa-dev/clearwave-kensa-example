import { useState } from "react"
import "./App.css"
import { Header } from "./components/Header"
import { FeasibilityForm } from "./components/FeasibilityForm"
import { ResultsPanel } from "./components/ResultsPanel"
import type { FeasibilityResponse } from "./lib/types"

function App() {
  const [result, setResult] = useState<FeasibilityResponse | null>(null)
  const [hasError, setHasError] = useState(false)
  const [hasSearched, setHasSearched] = useState(false)

  function handleResult(response: FeasibilityResponse | null, error: boolean) {
    setResult(response)
    setHasError(error)
    setHasSearched(true)
  }

  return (
    <div className="cw-root">
      <div className="cw-layout">
        <Header />
        <main className="cw-main">
          <FeasibilityForm onResult={handleResult} />
          {hasSearched && (
            <ResultsPanel response={result} hasError={hasError} />
          )}
        </main>
      </div>
    </div>
  )
}

export default App
