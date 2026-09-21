"use client";

import React from "react";
import { usePathname } from "next/navigation";
import BottomNav from "@/components/navigation/BottomNav";

export default function AppShell({ children }) {
  const pathname = usePathname();

  // 하단 네비게이션 바를 숨길 경로 (로그인, 회원가입 화면)
  const hideBottomNav =
    pathname === "/signup" ||
    pathname === "/signin" ||
    pathname === "/login";

  return (
    <div className="min-h-screen bg-[#f2f4f6] text-[#191f28] flex flex-col justify-start selection:bg-[#fff1f3] selection:text-[#85132d]">
      <div className="w-full max-w-md min-h-screen bg-white mx-auto shadow-[0_0_50px_rgba(0,0,0,0.05)] flex flex-col relative">
        <main className="flex-1 flex flex-col">{children}</main>
        {!hideBottomNav && <BottomNav />}
      </div>
    </div>
  );
}
