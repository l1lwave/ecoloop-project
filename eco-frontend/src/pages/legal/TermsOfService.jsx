import { useNavigate } from 'react-router-dom'

/**
 * Terms of Service.
 *
 * ⚠️ TEMPLATE / DRAFT — NOT LEGAL ADVICE. See the note at the top of
 * PrivacyPolicy.jsx — the same caveat applies here. Have this reviewed by a
 * qualified lawyer before relying on it, and fill in every [BRACKETED] value.
 */

const LAST_UPDATED = '[DD Month YYYY]'

export default function TermsOfService() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-eco-50 pb-16">
      <div className="bg-white px-4 pt-10 pb-4 shadow-sm">
        <button onClick={() => navigate(-1)} className="inline-flex min-h-[44px] items-center text-eco-700 text-sm font-medium mb-2">
          ← Back
        </button>
        <h1 className="text-xl font-bold text-gray-900">Terms of Service</h1>
        <p className="text-xs text-gray-400 mt-1">Last updated: {LAST_UPDATED}</p>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-6 text-sm text-gray-700 leading-relaxed space-y-8">

        <section>
          <p>
            These Terms of Service ("Terms") govern your use of the EcoLoop bottle-return and
            supermarket-locator web application (the "Service"), provided by{' '}
            <strong>[LEGAL ENTITY NAME]</strong>, registered address{' '}
            <strong>[REGISTERED ADDRESS, THE NETHERLANDS]</strong>, KVK number{' '}
            <strong>[KVK NUMBER]</strong> ("we", "us"). By creating an account or otherwise using the
            Service, you agree to these Terms. If you do not agree, do not use the Service.
          </p>
        </section>

        <Section title="1. The Service">
          <p>
            The Service helps users locate nearby supermarkets and deposit-return ("statiegeld")
            points in the Netherlands, scan product barcodes to check deposit/recyclability
            information, and track personal recycling statistics and achievement badges.
          </p>
        </Section>

        <Section title="2. Eligibility and accounts">
          <p>
            You must be able to form a binding contract under Dutch law to create an account. You are
            responsible for maintaining the confidentiality of your login credentials and for all
            activity under your account. Notify us immediately at <strong>[CONTACT EMAIL]</strong> if
            you suspect unauthorized use of your account.
          </p>
        </Section>

        <Section title="3. Permitted use">
          <p>You agree to use the Service only for its intended, lawful purpose. You agree not to:</p>
          <ul className="list-disc pl-5 space-y-1 mt-2">
            <li>Scrape, systematically extract, or bulk-download data from the Service or the third-party APIs it relies on, other than through the Service's own intended user interface;</li>
            <li>Attempt to interfere with, disrupt, or gain unauthorized access to the Service, its infrastructure, or other users' accounts;</li>
            <li>Submit false, fraudulent, or manipulated barcode/deposit data in order to obtain points, rewards, or badges you have not genuinely earned;</li>
            <li>Use the Service in any way that violates applicable Dutch or EU law.</li>
          </ul>
        </Section>

        <Section title="4. Third-party & community data — “as is”, “as available”">
          <p>
            Store locations, opening hours, accepted packaging materials, bottle-return machine
            presence/status, and similar location information displayed in the Service are sourced, in
            whole or in part, from community-maintained open datasets — principally{' '}
            <strong>OpenStreetMap</strong> (via the Overpass API and Nominatim) — and, separately,
            product and packaging information is sourced from <strong>Open Food Facts</strong>, another
            open, community-editable database. Deposit values, recyclability status, and packaging
            classifications shown for a scanned product reflect the data available from these sources
            (and our own product mapping) at the time of the request.
          </p>
          <p className="mt-2 font-semibold">
            THIS INFORMATION IS PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS, WITHOUT WARRANTIES OF
            ANY KIND, EXPRESS OR IMPLIED, AS TO ITS ACCURACY, COMPLETENESS, OR CURRENTNESS. Community
            datasets can be outdated, incomplete, or incorrect (for example: a store may have closed,
            changed its accepted materials, or no longer operate the return machine shown; a product's
            packaging or deposit classification may have changed since it was last recorded). We do not
            guarantee that any specific store, machine, opening hour, or deposit value shown is accurate
            at the moment you rely on it, and you should verify material information (e.g. whether a
            specific return point is currently operating) independently before relying on it, in
            particular before traveling specifically to a location shown in the Service.
          </p>
        </Section>

        <Section title="5. No warranty; limitation of liability">
          <p>
            To the maximum extent permitted by applicable Dutch law, the Service is provided without
            warranty of any kind, and we disclaim all warranties, whether express, implied, or
            statutory, including implied warranties of merchantability, fitness for a particular
            purpose, and non-infringement. We are not liable for any indirect, incidental, special, or
            consequential damages, or for loss of time, fuel, or opportunity, arising from reliance on
            inaccurate or outdated third-party/community data displayed in the Service, or from
            temporary unavailability of the Service or of the third-party APIs it depends on. Nothing in
            these Terms excludes or limits liability that cannot lawfully be excluded or limited under
            Dutch law (such as liability for intent or gross negligence, or death or personal injury
            caused by our negligence).
          </p>
        </Section>

        <Section title="6. Intellectual property & trademarks">
          <p>
            The Service's own software, design, and content are owned by us or our licensors. Map data
            is © OpenStreetMap contributors, available under the Open Database License (ODbL); product
            data is from Open Food Facts, likewise available under the ODbL and the Database Contents
            License. See the in-app <strong>Legal Attributions</strong> notice for full attribution.
          </p>
          <p className="mt-2">
            Supermarket and retailer names, logos, and brands referenced or displayed in the Service
            (including, without limitation, Albert Heijn, Jumbo, Lidl, Aldi, Plus, Spar, Coop, Dirk,
            Vomar, and DekaMarkt) are the trademarks and property of their respective owners. Their use
            in the Service is solely for informational identification of publicly known store locations
            and does not imply any sponsorship, endorsement, affiliation, or partnership between those
            brands and us.
          </p>
        </Section>

        <Section title="7. Termination">
          <p>
            You may stop using the Service or delete your account at any time (see the Privacy Policy
            for how to request erasure). We may suspend or terminate access to the Service, in whole or
            in part, for violation of these Terms or for operational/security reasons, with notice where
            reasonably practicable.
          </p>
        </Section>

        <Section title="8. Changes to the Service or these Terms">
          <p>
            We may modify or discontinue features of the Service, and may update these Terms from time
            to time. Continued use of the Service after an update constitutes acceptance of the revised
            Terms. Material changes will be highlighted in the app.
          </p>
        </Section>

        <Section title="9. Governing law and disputes">
          <p>
            These Terms are governed by the laws of the Netherlands, without regard to conflict-of-law
            principles. If you are a consumer resident in the EU, this does not deprive you of the
            protection afforded by mandatory provisions of the law of your country of residence. Any
            dispute arising out of or in connection with these Terms shall be submitted to the
            competent court of <strong>[COMPETENT DUTCH COURT / DISTRICT, e.g. the district court where
            the Controller is registered]</strong>, unless mandatory consumer-protection rules provide
            otherwise. EU consumers may also use the European Commission's{' '}
            <a href="https://ec.europa.eu/consumers/odr" target="_blank" rel="noopener noreferrer" className="text-eco-600 underline">
              Online Dispute Resolution platform
            </a>.
          </p>
        </Section>

        <Section title="10. Contact">
          <p><strong>[LEGAL ENTITY NAME]</strong> — <strong>[CONTACT EMAIL]</strong></p>
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
