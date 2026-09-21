"use client";

import React from "react";
import { User, LogIn, UserPlus, Globe, Shield, HelpCircle, LogOut } from "lucide-react";
import LanguageSelector from "@/components/common/LanguageSelector";

export default function MyPageView({ onNavigate, lang = "ko", onChangeLang, showToast }) {
  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] max-w-lg mx-auto w-full p-4 space-y-4 pb-20 animate-in fade-in">
      {/* 프로필 카드 */}
      <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-sm flex items-center gap-4">
        <div className="w-14 h-14 rounded-2xl bg-[#fff1f3] text-[#85132d] flex items-center justify-center font-bold text-xl">
          <User className="w-7 h-7" />
        </div>
        <div className="flex-1">
          <h2 className="text-base font-bold text-gray-900">어디갔지 체험 사용자</h2>
          <p className="text-xs text-gray-400 font-medium">user@whereisit.kr</p>
        </div>
      </div>

      {/* 계정 관리 섹션 */}
      <div className="bg-white rounded-2xl p-2 border border-gray-100 shadow-xs space-y-1">
        <button
          type="button"
          onClick={() => onNavigate("login")}
          className="w-full px-4 py-3 text-left text-xs sm:text-sm font-semibold text-gray-800 hover:bg-gray-50 rounded-xl flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <LogIn className="w-4 h-4 text-gray-400" />
            <span>로그인 화면 열기</span>
          </div>
          <span className="text-gray-400 text-xs">→</span>
        </button>

        <button
          type="button"
          onClick={() => onNavigate("signup")}
          className="w-full px-4 py-3 text-left text-xs sm:text-sm font-semibold text-gray-800 hover:bg-gray-50 rounded-xl flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <UserPlus className="w-4 h-4 text-gray-400" />
            <span>회원가입 화면 열기</span>
          </div>
          <span className="text-gray-400 text-xs">→</span>
        </button>
      </div>

      {/* 환경 설정 섹션 */}
      <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5 text-xs sm:text-sm font-semibold text-gray-800">
            <Globe className="w-4 h-4 text-gray-400" />
            <span>언어 설정</span>
          </div>
          <LanguageSelector currentLang={lang} onChangeLang={onChangeLang} variant="badge" />
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
  );
}
