import "./globals.css";
import { AppProvider } from "@/context/AppContext";
import AppShell from "@/components/layout/AppShell";
import { cookies } from "next/headers";

export const metadata = {
  title: "어디갔지 | AI 분실물 찾기",
  description: "한국에서 잃어버린 소중한 물건, AI와 함께 언제 어디서나 찾아요.",
  icons: {
    icon: [
      { url: "/icon.svg?v=3", type: "image/svg+xml" },
      { url: "/icon.png?v=3", sizes: "48x48", type: "image/png" },
      { url: "/favicon.ico?v=3", sizes: "any" },
    ],
    shortcut: "/favicon.ico?v=3",
    apple: "/icon.png?v=3",
  },
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export default async function RootLayout({ children }) {
  const cookieStore = await cookies();
  const initialLang = cookieStore.get("where_lang")?.value || "ko";

  return (
    <html lang={initialLang}>
      <body className="antialiased">
        <AppProvider initialLang={initialLang}>
          <AppShell>{children}</AppShell>
        </AppProvider>
      </body>
    </html>
  );
}
