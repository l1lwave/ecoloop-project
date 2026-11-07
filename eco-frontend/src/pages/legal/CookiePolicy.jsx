import { useNavigate } from 'react-router-dom'

/**
 * Cookie & Local Storage Policy.
 *
 * ⚠️ TEMPLATE / DRAFT — NOT LEGAL ADVICE. See PrivacyPolicy.jsx for the full caveat.
 *
 * Reflects actual current behavior (verified in the codebase): this app sets
 * NO cookies at all. Authentication uses a JWT kept in localStorage, and the
 * only other localStorage entry is the chosen UI language. If real cookies or
 * any analytics/marketing script are added later, this page (and
 * CookieConsent.jsx) MUST be updated first — under the ePrivacy Directive,
 * anything beyond strictly-necessary storage requires prior opt-in consent.
 */

const LAST_UPDATED = '[DD Month YYYY]'

export default function CookiePolicy() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-eco-50 pb-16">
      <div className="bg-white px-4 pt-10 pb-4 shadow-sm">
        <button onClick={() => navigate(-1)} className="inline-flex min-h-[44px] items-center text-eco-700 text-sm font-medium mb-2">
          ← Back
        </button>
        <h1 className="text-xl font-bold text-gray-900">Cookie &amp; Local Storage Policy</h1>
        <p className="text-xs text-gray-400 mt-1">Last updated: {LAST_UPDATED}</p>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-6 text-sm text-gray-700 leading-relaxed space-y-8">

        <section>
          <p>
            This page explains what we store in your browser and why, in line with the ePrivacy
            Directive (as implemented in the Dutch Telecommunications Act) and the GDPR.
          </p>
        </section>

        <Section title="1. We do not use cookies">
          <p>
            The Service does not set any HTTP cookies. Instead, it uses your browser's{' '}
            <code>localStorage</code> — a similar but distinct browser storage mechanism, also covered
            by Article 5(3) of the ePrivacy Directive whenever it is not strictly necessary for the
            service you requested.
          </p>
        </Section>

        <Section title="2. What we store in localStorage, and why">
          <table className="w-full text-xs border border-gray-200 rounded-lg overflow-hidden mt-2">
            <thead className="bg-gray-50 text-gray-500">
              <tr>
                <th className="text-left font-semibold px-3 py-2">Key</th>
                <th className="text-left font-semibold px-3 py-2">Purpose</th>
                <th className="text-left font-semibold px-3 py-2">Category</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              <tr>
                <td className="px-3 py-2 font-mono">eco_token</td>
                <td className="px-3 py-2">Keeps you logged in between visits (session/JWT).</td>
                <td className="px-3 py-2 text-eco-700 font-medium">Strictly necessary</td>
              </tr>
              <tr>
                <td className="px-3 py-2 font-mono">eco_user</td>
                <td className="px-3 py-2">Caches your basic account info so the app doesn't refetch it on every screen.</td>
                <td className="px-3 py-2 text-eco-700 font-medium">Strictly necessary</td>
              </tr>
              <tr>
                <td className="px-3 py-2 font-mono">eco_lang</td>
                <td className="px-3 py-2">Remembers your chosen interface language (NL / EN / UA).</td>
                <td className="px-3 py-2 text-eco-700 font-medium">Strictly necessary / functional</td>
              </tr>
            </tbody>
          </table>
          <p className="mt-2">
            None of this data is readable by any other website, is never transmitted to any analytics
            or advertising service, and clears when you log out (for <code>eco_token</code>/
            <code>eco_user</code>) or when you clear your browser's site data.
          </p>
        </Section>

        <Section title="3. Strictly necessary vs. non-essential storage">
          <p>
            Under the ePrivacy Directive, storage that is <strong>strictly necessary</strong> to
            provide a service you have explicitly requested (like staying logged in, or remembering a
            language you picked) is exempt from the opt-in consent requirement. All storage this app
            currently uses falls into that category, so no cookie consent is legally required for it —
            the consent banner you may see still tells you what's stored, for transparency, but it does
            not gate access to the app.
          </p>
          <p className="mt-2">
            <strong>Non-essential storage — such as analytics, advertising, or cross-site tracking
            cookies — is not currently used by this Service.</strong> If that ever changes, the new
            purpose will be added to the table above, described here, and — critically — will only
            begin after you actively opt in via the consent banner, as required by law. We will never
            silently start non-essential tracking without asking first.
          </p>
        </Section>

        <Section title="4. Third-party storage outside our control">
          <p>
            Map tiles and place search are loaded live from OpenStreetMap/Nominatim, and product lookups
            from Open Food Facts; these third-party services load resources directly from your browser
            and may set their own cookies/storage under their own policies, which we do not control. See
            our <a href="https://osmfoundation.org/wiki/Privacy_Policy" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">OpenStreetMap Foundation privacy policy</a> and{' '}
            <a href="https://world.openfoodfacts.org/privacy" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">Open Food Facts privacy policy</a> references.
          </p>
        </Section>

        <Section title="5. Managing or clearing storage">
          <p>
            You can clear <code>localStorage</code> for this site at any time via your browser's site
            settings ("Clear site data" / "Cookies and site data"), which will log you out and reset
            your language preference. Since none of it is a tracking cookie, there is no separate
            "reject" mechanism needed beyond your browser's own storage controls.
          </p>
        </Section>

      </div>
    </div>
  )
}

function Section({ title, children }) {
  return (
    <section>
      <h2 className="text-base font-bold text-gray-900 mb-2">{title}</h2>
      <div className="space-y-2">{children}</div>
    </section>
  )
}
