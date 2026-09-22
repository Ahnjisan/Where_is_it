"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Mail, Lock, Check, ChevronLeft, MapPin } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import LanguageSelector from "@/components/common/LanguageSelector";
import { useApp } from "@/context/AppContext";

export default function SignupPage() {
  const router = useRouter();
  const { lang, setLang, t, showToast, updateUser } = useApp();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!email) {
      showToast(lang === "ko" ? "이메일 주소를 입력해 주세요." : "Please enter your email.", "error");
      return;
    }
    // 이메일 형식 검사 (예: test@domain.com)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      showToast(lang === "ko" ? "이메일 주소 오류입니다." : "Invalid email address.", "error");
      return;
    }
    // 비밀번호 제약사항 검증: 최소 8자, 최대 20자, 형식 ^[!-~]{8,20}$ (ASCII 33~126: 영문, 숫자, 특수문자)
    const passwordRegex = /^[!-~]{8,20}$/;
    if (!password) {
      showToast(lang === "ko" ? "비밀번호를 입력해 주세요." : "Please enter your password.", "error");
      return;
    }
    if (!passwordRegex.test(password)) {
      showToast(
        lang === "ko"
          ? "비밀번호는 8~20자의 영문, 숫자, 특수문자만 사용 가능합니다."
          : "Password must be 8-20 characters (letters, numbers, special characters).",
        "error"
      );
      return;
    }
    if (password !== passwordConfirm) {
      showToast(
        lang === "ko" ? "비밀번호가 일치하지 않습니다." : "Passwords do not match.",
        "error"
      );
      return;
    }
    if (!agreeTerms) {
      showToast(
        lang === "ko" ? "약관에 동의해 주세요." : "Please agree to the Terms of Service.",
        "error"
      );
      return;
    }

    // 백엔드 API 연동 전 전달값 확인 콘솔 로그
    console.log("[Signup] 회원가입 요청 데이터:", {
      email,
      password,
      passwordConfirm,
      agreeTerms,
    });

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      const today = new Date();
      const formattedDate = `${today.getFullYear()}.${String(today.getMonth() + 1).padStart(2, "0")}.${String(today.getDate()).padStart(2, "0")}`;
      if (updateUser) {
        updateUser({
          email: email.trim(),
          joinedDate: formattedDate,
        });
      }
      showToast(
        lang === "ko" ? "회원가입이 성공적으로 완료되었습니다!" : "Account created successfully!",
        "success"
      );
      router.push("/");
    }, 600);
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

      {/* 로고 & 타이틀 (로그인 페이지와 동일한 로고 이미지 & 스타일) */}
      <div className="my-3 flex flex-col items-center text-center select-none">
        <div className="w-14 h-14 rounded-full bg-[#85132d] flex items-center justify-center text-white mb-4 shadow-sm">
          <MapPin className="w-7 h-7 fill-white" />
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#191f28] mb-2">
          {lang === "ko" ? "어디갔지" : "Where Is It"}
        </h1>
        {t.appSubSignup && (
          <p className="text-sm sm:text-base text-gray-500 font-medium max-w-xs leading-relaxed">
            {t.appSubSignup}
          </p>
        )}
      </div>

      {/* 회원가입 폼 */}
      <form onSubmit={handleSubmit} noValidate className="flex-1 flex flex-col justify-between max-w-sm mx-auto w-full">
        <div className="space-y-3.5 my-2">
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
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              maxLength={20}
              placeholder={lang === "ko" ? "비밀번호 (8~20자)" : "Password (8-20 chars)"}
              className="w-full h-13 pl-11 pr-4 rounded-2xl bg-gray-50/90 border border-gray-200/80 text-sm font-medium text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all"
            />
          </div>

          {/* 비밀번호 확인 */}
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
              <Lock className="w-5 h-5" />
            </div>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              maxLength={20}
              placeholder={t.pwConfirmPlaceholder}
              className="w-full h-13 pl-11 pr-4 rounded-2xl bg-gray-50/90 border border-gray-200/80 text-sm font-medium text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all"
            />
          </div>

          {/* 사용 언어 드롭다운 (한국어 / English 제한) */}
          <LanguageSelector
            currentLang={lang}
            onChangeLang={setLang}
            variant="form"
          />

          {/* 이용약관 동의 체크박스 */}
          <label className="flex items-start gap-3 pt-2 cursor-pointer select-none group">
            <div
              className={`w-5 h-5 rounded-lg border flex items-center justify-center transition-all mt-0.5 ${
                agreeTerms
                  ? "bg-[#85132d] border-[#85132d] text-white"
                  : "bg-white border-gray-300 group-hover:border-gray-400"
              }`}
              onClick={() => setAgreeTerms(!agreeTerms)}
            >
              {agreeTerms && <Check className="w-3.5 h-3.5 stroke-[3]" />}
            </div>
            <span className="text-xs sm:text-sm text-gray-600 font-medium leading-snug">
              {t.agreeTerms}
            </span>
          </label>
        </div>

        {/* 하단 버튼 및 로그인 이동 링크 */}
        <div className="pt-6 pb-6 space-y-4">
          <button
            type="submit"
            disabled={isLoading}
            className="w-full h-14 rounded-2xl bg-[#85132d] hover:bg-[#701025] active:scale-[0.98] text-white font-bold text-base shadow-md shadow-[#85132d]/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70"
          >
            {isLoading ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              t.signup
            )}
          </button>

          <div className="text-center">
            <Link
              href="/signin"
              className="text-xs sm:text-sm font-semibold text-gray-500 hover:text-[#85132d] transition-colors"
            >
              {t.alreadyHaveAccount}
            </Link>
          </div>
        </div>
      </form>
    </div>
  );
}
