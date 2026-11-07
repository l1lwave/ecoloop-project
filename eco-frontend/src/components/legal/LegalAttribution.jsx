import { Link } from 'react-router-dom'

/**
 * Mandatory third-party attributions and trademark disclaimers.
 *
 * - OpenStreetMap's tile usage policy and the Open Database License (ODbL)
 *   require visible "© OpenStreetMap contributors" attribution wherever the
 *   map/data is displayed — this is not optional legal decoration.
 * - Open Food Facts data is likewise ODbL + Database Contents License.
 * - Chain/brand names shown in the app (Albert Heijn, Jumbo, Lidl, Aldi, ...)
 *   are third-party trademarks, used only to identify real, publicly known
 *   store locations — this component makes that disclaimer explicit.
 *
 * variant="compact" — small, unobtrusive overlay for on top of the live map
 *   (use this wherever a Leaflet <TileLayer> has attributionControl={false}).
 * variant="full" (default) — complete footer block for legal/settings pages.
 */
export default function LegalAttribution({ variant = 'full' }) {
  if (variant === 'compact') {
    return (
      <div className="bg-white/80 backdrop-blur-sm rounded-full px-2.5 py-1 text-[10px] text-gray-600 shadow-sm pointer-events-auto">
        <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer" className="hover:underline">
          © OpenStreetMap contributors
        </a>
      </div>
    )
  }

  return (
    <div className="text-xs text-gray-400 leading-relaxed space-y-2 px-4 py-4">
      <p>
        Map data © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer" className="underline hover:text-gray-600">OpenStreetMap contributors</a>,
        available under the{' '}
        <a href="https://opendatacommons.org/licenses/odbl/" target="_blank" rel="noopener noreferrer" className="underline hover:text-gray-600">
          Open Database License (ODbL)
        </a>. Search powered by Nominatim; store/POI data via the Overpass API.
      </p>
      <p>
        Product data from{' '}
        <a href="https://world.openfoodfacts.org" target="_blank" rel="noopener noreferrer" className="underline hover:text-gray-600">Open Food Facts</a>,
        available under the{' '}
        <a href="https://opendatacommons.org/licenses/odbl/" target="_blank" rel="noopener noreferrer" className="underline hover:text-gray-600">ODbL</a>
        {' '}and{' '}
        <a href="https://opendatacommons.org/licenses/dbcl/1-0/" target="_blank" rel="noopener noreferrer" className="underline hover:text-gray-600">
          Database Contents License
        </a>.
      </p>
      <p>
        Supermarket and retailer names, logos, and brands referenced in this app (including, without
        limitation, Albert Heijn, Jumbo, Lidl, Aldi, Plus, Spar, Coop, Dirk, Vomar, and DekaMarkt) are
        trademarks of their respective owners, used solely to identify publicly known store locations.
        Their use does not imply sponsorship, endorsement, or affiliation.
      </p>
      <p className="pt-1">
        <Link to="/privacy" className="underline hover:text-gray-600">Privacy Policy</Link>
        {' · '}
        <Link to="/terms" className="underline hover:text-gray-600">Terms of Service</Link>
        {' · '}
        <Link to="/cookies" className="underline hover:text-gray-600">Cookie Policy</Link>
      </p>
    </div>
  )
}
