"use client";

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  User,
  LogIn,
  UserPlus,
  Globe,
  Shield,
  Bell,
  Sparkles
} from "lucide-react";
import LanguageSelector from "@/components/common/LanguageSelector";
import { useApp } from "@/context/AppContext";

export default function MyPage() {
  const router = useRouter();
  const { lang, setLang } = useApp();

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-20">
      {/* 상단 네비게이션 헤더 */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
            aria-label="뒤로가기"
          >
            <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
          </button>
          <h1 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
            마이페이지
          </h1>
        </div>

        <LanguageSelector
          currentLang={lang}
          onChangeLang={setLang}
          variant="badge"
        />
      </header>

      <div className="p-4 space-y-4">
        {/* 프로필 카드 */}
        <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-2xs flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-[#fff1f3] text-[#85132d] flex items-center justify-center font-bold text-xl shrink-0">
            <User className="w-7 h-7" />
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-base font-bold text-gray-900 truncate">
              어디갔지 체험 사용자
            </h2>
            <p className="text-xs text-gray-400 font-medium truncate">
              user@whereisit.kr
            </p>
          </div>
        </div>

        {/* 계정 관리 섹션 */}
        <div className="bg-white rounded-2xl p-2 border border-gray-100 shadow-2xs space-y-1">
          <Link
            href="/signin"
            className="w-full px-4 py-3.5 text-left text-xs sm:text-sm font-semibold text-gray-800 hover:bg-gray-50 rounded-xl flex items-center justify-between transition-colors"
          >
            <div className="flex items-center gap-3">
              <LogIn className="w-4 h-4 text-gray-400" />
              <span>로그인 페이지로 이동</span>
            </div>
            <span className="text-gray-400 text-xs">→</span>
          </Link>

          <Link
            href="/signup"
            className="w-full px-4 py-3.5 text-left text-xs sm:text-sm font-semibold text-gray-800 hover:bg-gray-50 rounded-xl flex items-center justify-between transition-colors"
          >
            <div className="flex items-center gap-3">
              <UserPlus className="w-4 h-4 text-gray-400" />
              <span>회원가입 페이지로 이동</span>
            </div>
            <span className="text-gray-400 text-xs">→</span>
          </Link>
        </div>

        {/* 환경 설정 섹션 */}
        <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5 text-xs sm:text-sm font-semibold text-gray-800">
              <Globe className="w-4 h-4 text-gray-400" />
              <span>언어 설정 (한국어 / English)</span>
            </div>
            <LanguageSelector
              currentLang={lang}
              onChangeLang={setLang}
              variant="badge"
            />
          </div>

          <div className="pt-2 border-t border-gray-50 flex items-center justify-between text-xs sm:text-sm font-semibold text-gray-800">
            <div className="flex items-center gap-2.5">
              <Shield className="w-4 h-4 text-gray-400" />
              <span>개인정보 및 이용약관</span>
            </div>
            <span className="text-gray-400 text-xs">보기</span>
          </div>
        </div>
      </div>
    </div>
  );
}
