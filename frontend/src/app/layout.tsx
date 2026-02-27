import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'SkyPath — Flight Connection Search',
  description: 'Search for flight itineraries with smart multi-stop connections, timezone-aware scheduling, and real-time results.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
