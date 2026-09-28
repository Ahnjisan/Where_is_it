"use client";

import React, { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import BottomNav from "@/components/navigation/BottomNav";
import { useApp } from "@/context/AppContext";

export default function AppShell({ children }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user } = useApp();
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    setIsMounted(true);
  }, []);

  useEffect(() => {
    if (!isMounted) return;

    const isAuthPage = pathname === "/signup" || pathname === "/signin" || pathname === "/login";
    const isRoot = pathname === "/";

    if (!user) {
      // 비회원인 경우: 루트(/)와 인증 페이지를 제외하고 모두 로그인으로 리다이렉트
      if (!isRoot && !isAuthPage) {
        router.replace("/signin");
      }
    } else {
      // 회원인 경우: 인증 페이지 접속 시 루트(/)로 리다이렉트
      if (isAuthPage) {
        router.replace("/");
      }
    }
  }, [user, pathname, router, isMounted]);

  // 하단 네비게이션 바를 숨길 경로 (로그인, 회원가입 화면)
  const hideBottomNav =
    pathname === "/signup" ||
    pathname === "/signin" ||
    pathname === "/login";

  // Hydration 이슈 방지를 위해 렌더링 지연을 선택적으로 할 수 있지만 여기선 일단 진행
  // if (!isMounted) return null;

  return (
    <div className="min-h-screen bg-[#f2f4f6] text-[#191f28] flex flex-col justify-start selection:bg-[#fff1f3] selection:text-[#85132d]">
      <div className="w-full max-w-md min-h-screen bg-white mx-auto shadow-[0_0_50px_rgba(0,0,0,0.05)] flex flex-col relative">
        <main className="flex-1 flex flex-col">{children}</main>
        {!hideBottomNav && <BottomNav />}
      </div>
    </div>
  );
}

