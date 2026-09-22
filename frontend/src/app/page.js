"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Send, User } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import LanguageSelector from "@/components/common/LanguageSelector";
import { EXAMPLE_PROMPTS_BY_LANG } from "@/lib/mockData";
import { useApp } from "@/context/AppContext";

export default function HomePage() {
  const router = useRouter();
  const { lang, setLang, t, showToast } = useApp();

  const [query, setQuery] = useState("");
  const examplePrompts = EXAMPLE_PROMPTS_BY_LANG[lang] || EXAMPLE_PROMPTS_BY_LANG.ko;

  const handleSend = () => {
    console.log("[HomePage] handleSend triggered with query:", query);
    if (!query.trim()) {
      console.log("[HomePage] query is empty, showing toast");
      showToast(
        lang === "ko"
          ? "분실물에 대한 내용을 입력해 주세요."
          : "Please describe your lost item.",
        "info"
      );
      return;
    }

    const searchQueryText = query.trim() || (lang === "ko" ? "지갑" : "Wallet");
    console.log("[HomePage] Navigating to search with query:", searchQueryText);
    router.push(`/search?q=${encodeURIComponent(searchQueryText)}`);
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      console.log("[HomePage] Enter key pressed in search input");
      handleSend();
    }
  };

  return (
    <div className="flex-1 flex flex-col justify-between animate-in fade-in duration-200">
      {/* 상단 네비게이션 헤더 (와이어프레임 03) */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <Link href="/" className="cursor-pointer" onClick={() => console.log("[HomePage] Logo clicked")}>
          <BrandLogo size="sm" lang={lang} />
        </Link>

        <div className="flex items-center gap-2">
          <LanguageSelector
            currentLang={lang}
            onChangeLang={(newLang) => {
              console.log("[HomePage] Language changed to:", newLang);
              setLang(newLang);
            }}
            variant="badge"
          />
          <Link
            href="/my"
            onClick={() => console.log("[HomePage] MyPage link clicked")}
            className="p-2 rounded-full hover:bg-gray-100 text-gray-600 hover:text-gray-900 transition-all active:scale-95"
            aria-label="마이페이지"
          >
            <User className="w-5 h-5" />
          </Link>
        </div>
      </header>

      {/* 중앙 메인 컨텐츠 영역 */}
      <div className="flex-1 flex flex-col justify-between px-5 py-6">
        {/* 질문 헤드라인 & 서브타이틀 */}
        <div className="mt-4 space-y-3">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#191f28] tracking-tight leading-tight">
            {t.mainQuestion}
          </h1>
          <p className="text-sm sm:text-base text-gray-500 font-medium leading-relaxed whitespace-pre-line">
            {t.mainSub}
          </p>
        </div>

        {/* 예시 질문 안내 섹션 (정적 안내형) */}
        <div className="my-6 space-y-3">
          <span className="text-xs font-semibold text-gray-400 block tracking-wide">
            {t.examplePromptLabel}
          </span>
          <div className="flex flex-col items-start gap-2">
            {examplePrompts.map((prompt, index) => (
              <div
                key={index}
                className="w-fit max-w-full px-3.5 py-2 rounded-xl bg-gray-100 border border-gray-200 text-xs sm:text-sm font-medium text-gray-700 flex items-center select-none"
              >
                <span className="truncate">&ldquo;{prompt}&rdquo;</span>
              </div>
            ))}
          </div>
        </div>

        {/* 대화형 검색 입력창 (ChatGPT / Gemini 스타일) */}
        <div className="mt-auto pt-4">
          <div className="bg-white rounded-3xl p-3.5 shadow-lg shadow-black/[0.04] border border-gray-200/90 focus-within:border-[#85132d] focus-within:ring-2 focus-within:ring-[#85132d]/15 transition-all">
            {/* 텍스트에어리어 */}
            <textarea
              value={query}
              onChange={(e) => {
                if (e.target.value.length <= 500) {
                  setQuery(e.target.value);
                }
              }}
              onKeyDown={handleKeyDown}
              placeholder={t.searchPlaceholder}
              rows={3}
              className="w-full resize-none bg-transparent border-none text-sm sm:text-base font-medium text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-0 leading-relaxed px-1"
            />

            {/* 하단 툴바 */}
            <div className="flex items-center justify-between pt-2 mt-1 border-t border-gray-100/80">
              <div className="text-xs text-gray-400 pl-1">
                {t.enterToSearch}
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs font-medium text-gray-400 select-none">
                  {query.length}/500
                </span>

                <button
                  type="button"
                  onClick={handleSend}
                  disabled={!query.trim()}
                  className="w-9 h-9 rounded-full bg-[#85132d] hover:bg-[#701025] disabled:bg-gray-200 disabled:cursor-not-allowed text-white flex items-center justify-center transition-all active:scale-90 shadow-sm cursor-pointer"
                  aria-label="전송"
                >
                  <Send className="w-4 h-4 -translate-x-0.5 translate-y-0.5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
