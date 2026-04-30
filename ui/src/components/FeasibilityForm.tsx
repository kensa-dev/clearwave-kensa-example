import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Loader2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import type { FeasibilityResponse, ServiceAddress } from "@/lib/types"
import { checkFeasibility } from "@/lib/api"

const schema = z.object({
  postcode: z.string().refine((v) => v.trim().length > 0, {
    message: "Postcode is required",
  }),
  addressLine1: z.string(),
  town: z.string(),
  county: z.string(),
})

type FormValues = z.infer<typeof schema>

interface FeasibilityFormProps {
  onResult: (result: FeasibilityResponse | null, error: boolean) => void
}

export function FeasibilityForm({ onResult }: FeasibilityFormProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      postcode: "",
      addressLine1: "",
      town: "",
      county: "",
    },
  })

  const isSubmitting = form.formState.isSubmitting

  async function onSubmit(values: FormValues) {
    const address: ServiceAddress = {
      postcode: values.postcode.trim(),
      addressLine1: values.addressLine1,
      town: values.town,
      county: values.county,
    }
    try {
      const result = await checkFeasibility(address)
      onResult(result, false)
    } catch {
      onResult(null, true)
    }
  }

  return (
    <Card className="cw-form-card">
      <CardHeader>
        <CardTitle className="cw-form-title">Check your address</CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="cw-form-fields">
            <FormField
              control={form.control}
              name="postcode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Postcode</FormLabel>
                  <FormControl>
                    <Input data-testid="postcode-input" placeholder="e.g. SW1A 2AA" autoComplete="postal-code" {...field} />
                  </FormControl>
                  <FormMessage data-testid="postcode-error" />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="addressLine1"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Address line 1</FormLabel>
                  <FormControl>
                    <Input data-testid="address-line1-input" placeholder="Street address" autoComplete="address-line1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="town"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Town</FormLabel>
                  <FormControl>
                    <Input data-testid="town-input" placeholder="Town or city" autoComplete="address-level2" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="county"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>County</FormLabel>
                  <FormControl>
                    <Input data-testid="county-input" placeholder="County (optional)" autoComplete="address-level1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button
              data-testid="check-button"
              type="submit"
              disabled={isSubmitting}
              className="cw-submit-btn"
            >
              {isSubmitting && <Loader2 className="animate-spin" />}
              {isSubmitting ? "Checking…" : "Check availability"}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
