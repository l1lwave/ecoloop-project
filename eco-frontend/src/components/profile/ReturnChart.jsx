import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { buildMonthlySeries } from './historyFormat'

const ITEMS_COLOR   = '#16a34a' // eco-600, the profile theme colour
const DEPOSIT_COLOR = '#d97706' // amber-600, separates the money line from the item bars

// Monthly chart: bars for items returned, a line for deposit value (EUR).
// ResponsiveContainer keeps the chart within the card width on phones.
export default function ReturnChart({ year, months }) {
  const { t, i18n } = useTranslation()
  const data = useMemo(
    () => buildMonthlySeries(year, months, i18n.language),
    [year, months, i18n.language],
  )

  return (
    <div role="img" aria-label={t('history.chartAria', { year })} className="mt-4 w-full">
      <ResponsiveContainer width="100%" height={220}>
        <ComposedChart data={data} margin={{ top: 8, right: 4, bottom: 0, left: -12 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
          <XAxis dataKey="label" interval={0} tick={{ fill: '#6b7280', fontSize: 9 }} />
          <YAxis yAxisId="items" allowDecimals={false} width={28} tick={{ fill: '#6b7280', fontSize: 10 }} />
          <YAxis yAxisId="deposit" orientation="right" width={36} tick={{ fill: '#6b7280', fontSize: 10 }} />
          <Tooltip />
          <Legend iconSize={8} />
          <Bar
            yAxisId="items"
            dataKey="items"
            name={t('history.chartItems')}
            fill={ITEMS_COLOR}
            radius={[4, 4, 0, 0]}
          />
          <Line
            yAxisId="deposit"
            type="monotone"
            dataKey="deposit"
            name={t('history.chartDeposit')}
            stroke={DEPOSIT_COLOR}
            strokeWidth={2}
            dot={false}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}
