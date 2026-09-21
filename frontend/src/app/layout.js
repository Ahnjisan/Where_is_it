import "./globals.css";
import { AppProvider } from "@/context/AppContext";
import AppShell from "@/components/layout/AppShell";

export const metadata = {
  title: "어디갔지 | AI 분실물 찾기",
  description: "한국에서 잃어버린 소중한 물건, AI와 함께 언제 어디서나 찾아요.",
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body className="antialiased">
        <AppProvider>
          <AppShell>{children}</AppShell>
        </AppProvider>
      </body>
    </html>
  );
}
