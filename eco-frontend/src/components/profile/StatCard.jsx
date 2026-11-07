// Single labelled metric tile used on the profile page.
export default function StatCard({ icon, label, value }) {
  return (
    <div className="bg-white rounded-2xl p-4 shadow-sm flex items-center gap-3">
      <span className="text-2xl">{icon}</span>
      <div>
        <p className="text-gray-500 text-xs">{label}</p>
        <p className="font-bold text-gray-900">{value}</p>
      </div>
    </div>
  )
}
