"use client";

import React, { useState } from "react";
import { Mail, Lock, Eye, EyeOff, Check, ChevronLeft } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import { I18N } from "@/lib/mockData";

export default function LoginView({ onNavigate, lang = "ko", showToast }) {
  const t = I18N[lang] || I18N.ko;

  const [email, setEmail] = useState("user@whereisit.kr");
  const [password, setPassword] = useState("password1234");
  const [showPassword, setShowPassword] = useState(false);
  const [keepLoggedIn, setKeepLoggedIn] = useState(true);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!email) {
      showToast(lang === "ko" ? "이메일 주소를 입력해 주세요." : "Please enter your email.", "error");
      return;
    }
    if (!password) {
      showToast(lang === "ko" ? "비밀번호를 입력해 주세요." : "Please enter your password.", "error");
      return;
    }

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      showToast(
        lang === "ko" ? "반갑습니다! 로그인되었습니다." : "Welcome back! Successfully logged in.",
        "success"
      );
      onNavigate("main");
    }, 500);
  };

  return (
    <div className="flex flex-col min-h-full bg-white px-6 py-4 animate-in fade-in duration-300">
      {/* 상단 네비게이션 */}
      <div className="flex items-center justify-between mb-2">
        <button
          type="button"
          onClick={() => onNavigate("main")}
          className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95"
          aria-label="뒤로가기"
        >
          <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
        </button>
      </div>

      {/* 로고 & 타이틀 */}
      <div className="my-6">
        <BrandLogo
          size="md"
          subtitle={t.appSubLogin}
          align="center"
          lang={lang}
        />
      </div>

      {/* 로그인 폼 */}
      <form onSubmit={handleSubmit} className="flex-1 flex flex-col justify-between max-w-sm mx-auto w-full">
        <div className="space-y-4 my-2">
          {/* 이메일 */}
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
              <Mail className="w-5 h-5" />
            </div>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={t.emailPlaceholder}
              className="w-full h-13 pl-11 pr-4 rounded-2xl bg-gray-50/90 border border-gray-200/80 text-sm font-medium text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all"
            />
          </div>

          {/* 비밀번호 */}
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
              <Lock className="w-5 h-5" />
            </div>
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t.pwPlaceholderLogin}
              className="w-full h-13 pl-11 pr-11 rounded-2xl bg-gray-50/90 border border-gray-200/80 text-sm font-medium text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
              aria-label="비밀번호 보기 토글"
            >
              {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
            </button>
          </div>

          {/* 옵션 행: 로그인 상태 유지 & 비밀번호 찾기 */}
          <div className="flex items-center justify-between pt-1">
            <label className="flex items-center gap-2 cursor-pointer select-none group">
              <div
                className={`w-5 h-5 rounded-lg border flex items-center justify-center transition-all ${
                  keepLoggedIn
                    ? "bg-[#85132d] border-[#85132d] text-white"
                    : "bg-white border-gray-300 group-hover:border-gray-400"
                }`}
                onClick={() => setKeepLoggedIn(!keepLoggedIn)}
              >
                {keepLoggedIn && <Check className="w-3.5 h-3.5 stroke-[3]" />}
              </div>
              <span className="text-xs sm:text-sm text-gray-600 font-medium">
                {t.keepLoggedIn}
              </span>
            </label>

            <button
              type="button"
              onClick={() =>
                showToast(
                  lang === "ko"
                    ? "비밀번호 재설정 링크가 이메일로 전송되었습니다."
                    : "Password reset link has been sent to your email.",
                  "info"
                )
              }
              className="text-xs sm:text-sm text-gray-500 hover:text-gray-900 font-medium transition-colors"
            >
              {t.findPassword}
            </button>
          </div>
        </div>

        {/* 하단 액션 버튼 & 회원가입 전환 */}
        <div className="pt-8 pb-4 space-y-4">
          <button
            type="submit"
            disabled={isLoading}
            className="w-full h-14 rounded-2xl bg-[#85132d] hover:bg-[#701025] active:scale-[0.98] text-white font-bold text-base shadow-md shadow-[#85132d]/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70"
          >
            {isLoading ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              t.login
            )}
          </button>

          <div className="text-center">
            <button
              type="button"
              onClick={() => onNavigate("signup")}
              className="text-xs sm:text-sm font-semibold text-gray-500 hover:text-[#85132d] transition-colors"
            >
              {t.noAccount}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
