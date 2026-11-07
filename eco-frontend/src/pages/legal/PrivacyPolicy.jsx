import { Link, useNavigate } from 'react-router-dom'

/**
 * Privacy Policy — GDPR (Regulation (EU) 2016/679) Articles 12–14 information notice.
 *
 * ⚠️ TEMPLATE / DRAFT — NOT LEGAL ADVICE.
 * This is a starting point reflecting this app's actual, current data flows
 * (verified against the codebase at time of writing: no cookies, no analytics,
 * JWT-in-localStorage auth, live GPS used client-side only, Overpass/Nominatim/
 * Open Food Facts as the only third-party data recipients). Every [BRACKETED]
 * value is a placeholder. Have a qualified lawyer (ideally one familiar with
 * Dutch/EU data protection law) review this before it governs real users'
 * personal data — GDPR fines and liability are real and this document alone
 * does not make the underlying processing compliant if the placeholders are
 * left unfilled or the described processing drifts from what the app actually
 * does. Keep this file in sync whenever data flows change.
 */

const LAST_UPDATED = '[DD Month YYYY]'

export default function PrivacyPolicy() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-eco-50 pb-16">
      <div className="bg-white px-4 pt-10 pb-4 shadow-sm">
        <button onClick={() => navigate(-1)} className="inline-flex min-h-[44px] items-center text-eco-700 text-sm font-medium mb-2">
          ← Back
        </button>
        <h1 className="text-xl font-bold text-gray-900">Privacy Policy</h1>
        <p className="text-xs text-gray-400 mt-1">Last updated: {LAST_UPDATED}</p>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-6 text-sm text-gray-700 leading-relaxed space-y-8">

        <section>
          <p>
            This Privacy Policy explains how <strong>[LEGAL ENTITY NAME]</strong> ("we", "us", "the
            Controller"), established at <strong>[REGISTERED ADDRESS, THE NETHERLANDS]</strong> and
            registered with the Dutch Chamber of Commerce (KVK) under number <strong>[KVK NUMBER]</strong>,
            processes personal data in connection with the EcoLoop bottle-return and supermarket-locator
            web application (the "Service"). It is written to comply with Articles 12, 13 and 14 of the
            General Data Protection Regulation (EU) 2016/679 ("GDPR") and the Dutch Telecommunications Act
            (implementing the ePrivacy Directive) as applied in the Netherlands.
          </p>
        </section>

        <Section title="1. Who is responsible for your data (the Controller)">
          <p>Data Controller: <strong>[LEGAL ENTITY NAME]</strong></p>
          <p>Contact: <strong>[CONTACT EMAIL — e.g. privacy@ecoloop.nl]</strong></p>
          <p>
            Data Protection Officer (if appointed): <strong>[DPO NAME / "Not applicable — a DPO is not
            mandatory for this Service under Article 37 GDPR unless our processing scale changes"]</strong>,
            reachable at <strong>[DPO EMAIL]</strong>.
          </p>
        </Section>

        <Section title="2. What data we process, and why">
          <SubSection title="2.1 Location data (GPS)">
            <p>
              When you tap "My location" on the Map page, your browser's Geolocation API requests your
              device's coordinates directly. Those coordinates are used <strong>ephemerally, in your
              browser</strong>, to (a) center the map and (b) compute straight-line distances to nearby
              return points, and are sent, per search, to our <code>location-service</code> API purely as
              query parameters to run a "find nearby" database lookup. <strong>We do not write your GPS
              coordinates to any database, log file, or analytics system, and we never sell or share raw
              location data with third parties.</strong> If our own database has no coverage for your area,
              the same coordinates may instead be sent, over HTTPS, to a public OpenStreetMap Overpass API
              mirror to fetch nearby supermarkets live — see Section 4.
            </p>
            <p>
              Legal basis: Article 6(1)(b) GDPR (processing necessary to perform the map/locator feature
              you actively requested) and, for the browser's own permission prompt, your explicit consent
              at the operating-system/browser level (Article 6(1)(a)).
            </p>
          </SubSection>

          <SubSection title="2.2 Camera stream (barcode scanning)">
            <p>
              The Scan page uses your device camera, via the ZXing barcode-decoding library, to read
              product barcodes. <strong>Video frames are decoded entirely on your device, inside your
              browser; no image or video frame is ever uploaded, transmitted, or stored by us or any
              third party.</strong> Only the decoded barcode number (a string of digits, not personal data
              on its own) is sent to our <code>scan-service</code> API to look up product information.
            </p>
            <p>Legal basis: Article 6(1)(b) GDPR (necessary to provide the scanning feature you invoked) and your device-level camera permission grant.</p>
          </SubSection>

          <SubSection title="2.3 Account data">
            <p>
              If you register an account, we store your <strong>name, email address, and a securely
              hashed password</strong> (we never store your password in plain text) together with an
              account creation timestamp, so you can log in and keep your points/badges across sessions.
            </p>
            <p>Legal basis: Article 6(1)(b) GDPR (performance of the contract you enter into by creating an account) and Article 6(1)(f) (legitimate interest in securing accounts, e.g. password hashing).</p>
          </SubSection>

          <SubSection title="2.4 Activity & reward data">
            <p>
              To provide points, recycling statistics, and achievement badges, we store, linked to your
              account: barcodes you have deposited, timestamps, points earned, and which badges you have
              unlocked. This is the data behind your own "Profile / Stats" screen.
            </p>
            <p>Legal basis: Article 6(1)(b) GDPR (performance of the reward feature of the Service).</p>
          </SubSection>

          <SubSection title="2.5 Local storage (browser)">
            <p>
              We use your browser's <code>localStorage</code> — not cookies — to keep you logged in
              (a session token) and remember your chosen interface language. This data never leaves your
              device except when the token is sent, as an Authorization header, to authenticate your own
              API requests. See the <Link to="/cookies" className="text-eco-600 underline">Cookie &amp; Local Storage Policy</Link> for details.
            </p>
          </SubSection>

          <SubSection title="2.6 Server logs, IP addresses & session identifiers">
            <p>
              Like virtually all web services, our hosting infrastructure and reverse proxy/API gateway
              automatically record standard technical request logs (IP address, timestamp, requested
              path, HTTP status, user-agent string) for security, abuse prevention, and debugging. These
              logs are retained for a limited period (<strong>[RETENTION PERIOD, e.g. 30 days]</strong>)
              and then deleted or irreversibly anonymized. Authenticated requests also carry a JSON Web
              Token (JWT) session identifier, which is not a tracking cookie and is used solely to verify
              you are logged in.
            </p>
            <p>Legal basis: Article 6(1)(f) GDPR (legitimate interest in operating a secure, reliable, abuse-resistant service), proportionate to that purpose.</p>
          </SubSection>
        </Section>

        <Section title="3. We do not currently use analytics, advertising, or tracking">
          <p>
            At the time of writing, this Service integrates no analytics platform, advertising network,
            or cross-site tracking technology of any kind. If that changes in the future, this Policy and
            the cookie consent banner will be updated first, and any non-essential tracking will only run
            after you actively opt in, as required by the ePrivacy Directive and Article 6(1)(a) GDPR.
          </p>
        </Section>

        <Section title="4. Recipients of your data & international transfers">
          <p>We share limited data with the following categories of recipients, only as needed to run the features described above:</p>
          <ul className="list-disc pl-5 space-y-2 mt-2">
            <li>
              <strong>OpenStreetMap contributors / Overpass API</strong> — when our own database has no
              coverage for an area you search, your search coordinates (not your identity) are sent over
              HTTPS to a public Overpass API mirror (e.g. overpass-api.de, overpass.kumi.systems) to fetch
              nearby supermarkets, and to the OpenStreetMap tile servers to render map imagery, and to the
              Nominatim geocoding service to resolve place-name searches. These are independent,
              community-operated services governed by the{' '}
              <a href="https://osmfoundation.org/wiki/Privacy_Policy" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">
                OpenStreetMap Foundation's own privacy policy
              </a>. Depending on the mirror, servers may be located outside the EU/EEA; we select mirrors
              on a best-effort basis and do not control their infrastructure.
            </li>
            <li>
              <strong>Open Food Facts</strong> — when you scan a barcode, that barcode number (not your
              identity or location) is sent to the Open Food Facts public API to retrieve product
              information. See the{' '}
              <a href="https://world.openfoodfacts.org/privacy" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">
                Open Food Facts privacy policy
              </a>.
            </li>
            <li>
              <strong>Hosting / infrastructure provider</strong> — <strong>[HOSTING PROVIDER NAME, e.g.
              a named EU cloud/hosting company]</strong>, which stores our databases and serves the
              application. <strong>[State here whether the provider is EU/EEA-based; if any transfer
              outside the EU/EEA is involved, identify the safeguard relied upon — e.g. an EU Commission
              adequacy decision or Standard Contractual Clauses under Article 46 GDPR.]</strong>
            </li>
          </ul>
          <p className="mt-2">
            We do not sell personal data, and we do not share personal data with advertisers or data
            brokers.
          </p>
        </Section>

        <Section title="5. How long we keep your data">
          <ul className="list-disc pl-5 space-y-1">
            <li>Account data: for as long as your account is active, plus [PERIOD] after deletion for fraud-prevention/legal-obligation purposes.</li>
            <li>Activity/reward data: for as long as your account is active, so your points and badges remain accurate.</li>
            <li>Server/access logs: [RETENTION PERIOD, e.g. 30 days], then deleted or anonymized.</li>
            <li>GPS coordinates and camera frames: not retained at all — see Sections 2.1 and 2.2.</li>
          </ul>
        </Section>

        <Section title="6. Your rights under the GDPR">
          <p>Subject to the conditions set out in the GDPR, you have the right to:</p>
          <ul className="list-disc pl-5 space-y-1 mt-2">
            <li><strong>Access</strong> (Art. 15) — obtain a copy of the personal data we hold about you.</li>
            <li><strong>Rectification</strong> (Art. 16) — correct inaccurate or incomplete data.</li>
            <li><strong>Erasure</strong> (Art. 17) — request deletion of your account and associated data ("right to be forgotten").</li>
            <li><strong>Restriction of processing</strong> (Art. 18) — limit how we use your data in certain circumstances.</li>
            <li><strong>Data portability</strong> (Art. 20) — receive your data in a structured, machine-readable format, or have it transmitted to another controller.</li>
            <li><strong>Object</strong> (Art. 21) — object to processing based on our legitimate interests.</li>
            <li><strong>Withdraw consent</strong> at any time, where processing is based on consent (e.g. browser location/camera permissions), without affecting the lawfulness of processing before withdrawal.</li>
          </ul>
          <p className="mt-2">
            To exercise any of these rights, contact us at <strong>[CONTACT EMAIL]</strong>. We will
            respond within one month as required by Article 12(3) GDPR.
          </p>
          <p className="mt-2">
            You also have the right to lodge a complaint with the Dutch supervisory authority:
          </p>
          <p className="mt-1">
            <strong>Autoriteit Persoonsgegevens</strong><br />
            Bezuidenhoutseweg 30, 2594 AV Den Haag, The Netherlands<br />
            <a href="https://www.autoriteitpersoonsgegevens.nl" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">
              autoriteitpersoonsgegevens.nl
            </a>
          </p>
        </Section>

        <Section title="7. Security">
          <p>
            We apply appropriate technical and organizational measures under Article 32 GDPR, including
            password hashing, encrypted transport (HTTPS/TLS) for all API traffic, and token-based
            authentication. No system is perfectly secure; if we become aware of a personal data breach
            likely to result in a risk to your rights, we will notify the Autoriteit Persoonsgegevens
            within 72 hours as required by Article 33 GDPR and, where required, notify affected users
            under Article 34.
          </p>
        </Section>

        <Section title="8. Children">
          <p>
            The Service is not directed at children under 16. We do not knowingly collect personal data
            from children under that age without verifiable parental consent, as required under Dutch
            implementation of Article 8 GDPR.
          </p>
        </Section>

        <Section title="9. Changes to this Policy">
          <p>
            We may update this Policy as the Service evolves. Material changes will be highlighted in the
            app, and the "Last updated" date above will reflect the latest revision.
          </p>
        </Section>

        <Section title="10. Contact">
          <p>Questions about this Policy or your data: <strong>[CONTACT EMAIL]</strong>.</p>
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

function SubSection({ title, children }) {
  return (
    <div className="mb-3">
      <h3 className="text-sm font-semibold text-gray-800 mb-1">{title}</h3>
      <div className="space-y-1">{children}</div>
    </div>
  )
}
