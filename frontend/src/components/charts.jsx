import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import ChartCard from './ChartCard';
import { CHART_COLORS } from '../utils/chartColors';

const H = 260;

export function TimeSeriesChart({ title, description, data, xKey = 'date', yKey = 'count', yLabel = 'Count', xLabel = 'Date', xFormatter }) {
  return (
    <ChartCard title={title} description={description} rows={data} columns={[{ key: xKey, label: xLabel }, { key: yKey, label: yLabel }]}>
      <ResponsiveContainer width="100%" height={H}>
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis dataKey={xKey} tickFormatter={xFormatter} tick={{ fontSize: 12 }} minTickGap={24} />
          <YAxis allowDecimals={false} tick={{ fontSize: 12 }} width={32} />
          <Tooltip labelFormatter={xFormatter} />
          <Line type="monotone" dataKey={yKey} name={yLabel} stroke={CHART_COLORS[0]} strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function SimpleBarChart({ title, description, data, xKey, yKey = 'count', yLabel = 'Count', xLabel = 'Category', xFormatter, horizontal = false }) {
  const height = horizontal ? Math.max(H, data.length * 32 + 40) : H;
  return (
    <ChartCard title={title} description={description} rows={data} columns={[{ key: xKey, label: xLabel }, { key: yKey, label: yLabel }]}>
      <ResponsiveContainer width="100%" height={height}>
        {horizontal ? (
          <BarChart data={data} layout="vertical" margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} />
            <YAxis type="category" dataKey={xKey} width={130} tick={{ fontSize: 12 }} tickFormatter={xFormatter} />
            <Tooltip />
            <Bar dataKey={yKey} name={yLabel} fill={CHART_COLORS[1]} radius={[0, 4, 4, 0]} />
          </BarChart>
        ) : (
          <BarChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis dataKey={xKey} tickFormatter={xFormatter} tick={{ fontSize: 12 }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 12 }} width={32} />
            <Tooltip labelFormatter={xFormatter} />
            <Bar dataKey={yKey} name={yLabel} fill={CHART_COLORS[0]} radius={[4, 4, 0, 0]} />
          </BarChart>
        )}
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function DonutChart({ title, description, data, nameKey, valueKey = 'count', nameLabel = 'Category', valueLabel = 'Count' }) {
  return (
    <ChartCard title={title} description={description} rows={data} columns={[{ key: nameKey, label: nameLabel }, { key: valueKey, label: valueLabel }]}>
      <ResponsiveContainer width="100%" height={H}>
        <PieChart>
          <Pie data={data} dataKey={valueKey} nameKey={nameKey} innerRadius={50} outerRadius={90} paddingAngle={2}>
            {data.map((_, i) => <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}
