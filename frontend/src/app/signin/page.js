"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Mail, Lock, Eye, EyeOff, Check, ChevronLeft } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import { useApp } from "@/context/AppContext";

export default function SigninPage() {
  const router = useRouter();
  const { lang, t, showToast, updateUser } = useApp();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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

    // 이메일 형식 검사 (정규식) 또는 비밀번호 8자 이상 규칙 검증
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim()) || password.length < 8) {
      showToast(
        lang === "ko"
          ? "이메일 및 비밀번호 오류입니다."
          : "Invalid email or password.",
        "error"
      );
      return;
    }

    // 백엔드 API 연동 전 전달값 확인 콘솔 로그
    console.log("[Signin] 로그인 요청 데이터:", {
      email,
      password,
      keepLoggedIn,
    });

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      if (updateUser) {
        updateUser({ email: email.trim() });
      }
      showToast(
        lang === "ko" ? "로그인되었습니다. 환영합니다!" : "Welcome back! Successfully logged in.",
        "success"
      );
      router.push("/");
    }, 500);
  };

  return (
    <div className="flex flex-col min-h-screen bg-white px-6 py-4 animate-in fade-in duration-200">
      {/* 상단 뒤로가기 (<) */}
      <div className="flex items-center justify-between h-12">
        <button
          type="button"
          onClick={() => router.back()}
          className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95"
          aria-label="뒤로가기"
        >
          <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
        </button>
      </div>

      {/* 로고 & 타이틀 */}
      <div className="my-4">
        <BrandLogo
          size="md"
          subtitle={t.appSubLogin}
          align="center"
          lang={lang}
        />
      </div>

      {/* 로그인 폼 */}
      <form onSubmit={handleSubmit} noValidate className="flex-1 flex flex-col justify-between max-w-sm mx-auto w-full">
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
              placeholder={lang === "ko" ? "이메일 주소" : "Email address"}
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
              placeholder={lang === "ko" ? "비밀번호 (8자 이상)" : "Password (8+ characters)"}
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

          {/* 옵션 행: 로그인 상태 유지 */}
          <div className="flex items-center justify-start pt-1">
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
          </div>
        </div>

        {/* 하단 액션 버튼 & 회원가입 전환 링크 */}
        <div className="pt-8 pb-6 space-y-4">
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
            <Link
              href="/signup"
              className="text-xs sm:text-sm font-semibold text-gray-500 hover:text-[#85132d] transition-colors"
            >
              {t.noAccount}
            </Link>
          </div>
        </div>
      </form>
    </div>
  );
}
