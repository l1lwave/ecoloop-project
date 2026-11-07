import ReturnHistory from './ReturnHistory'
import ScanHistoryList from './ScanHistoryList'

// Fourth profile window: monthly return chart on top, paginated scan history below.
export default function HistoryPanel() {
  return (
    <div className="pb-2 space-y-4">
      <ReturnHistory />
      <ScanHistoryList />
    </div>
  )
}
