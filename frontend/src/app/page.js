"use client";

import React, { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { LoaderCircle, Send } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import { EXAMPLE_PROMPTS_BY_LANG } from "@/lib/mockData";
import { useApp } from "@/context/AppContext";
import { lostItemApi } from "@/lib/api";

export default function HomePage() {
  const router = useRouter();
  const {
    lang,
    t,
    showToast,
    user,
    logoutUser,
    setSearchExecution,
    clearSearchExecution,
  } = useApp();

  const [query, setQuery] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submittingRef = useRef(false);
  const examplePrompts =
    EXAMPLE_PROMPTS_BY_LANG[lang] || EXAMPLE_PROMPTS_BY_LANG.ko;

  const handleSend = async () => {
    if (submittingRef.current) return;

    if (!user?.accessToken) {
      showToast(lang === "ko" ? "로그인 후 이용해주세요." : "Please login to use this feature.", "info");
      router.push("/signin");
      return;
    }

    if (!query.trim()) {
      showToast(
        lang === "ko"
          ? "분실물에 대한 내용을 입력해 주세요."
          : "Please describe your lost item.",
        "info",
      );
      return;
    }

    const searchQueryText = query.trim();
    clearSearchExecution();
    submittingRef.current = true;
    setIsSubmitting(true);

    try {
      const result = await lostItemApi.createSearch(
        { description: searchQueryText, languageCode: lang },
        user.accessToken,
      );
      setSearchExecution(result);
      const lostItemId = result?.lostItem?.lostItemId;
      const baseSearchUrl = `/search?q=${encodeURIComponent(searchQueryText)}`;
      router.push(
        lostItemId ? `${baseSearchUrl}&lostItemId=${encodeURIComponent(lostItemId)}` : baseSearchUrl,
      );
    } catch (error) {
      if (error.status === 401) {
        await logoutUser();
        showToast(
          lang === "ko"
            ? "로그인이 만료되었습니다. 다시 로그인해 주세요."
            : "Your session has expired. Please sign in again.",
          "error",
        );
        router.push("/signin");
        return;
      }

      showToast(
        error.message ||
          (lang === "ko" ? "검색 중 오류가 발생했습니다." : "Search failed."),
        "error",
      );
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFocus = (e) => {
    if (!user) {
      e.target.blur();
      showToast(lang === "ko" ? "로그인 후 이용해주세요." : "Please login to use this feature.", "info");
      router.push("/signin");
    }
  };

  return (
    <div className="flex-1 flex flex-col justify-between animate-in fade-in duration-200">
      {/* 상단 네비게이션 헤더 (와이어프레임 03) */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <Link
          href="/"
          className="cursor-pointer"
          onClick={() => console.log("[HomePage] Logo clicked")}
        >
          <BrandLogo size="sm" lang={lang} />
        </Link>
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
                className="w-fit max-w-full px-3.5 py-2 rounded-xl bg-gray-100 border border-gray-200 text-xs sm:text-sm font-medium text-gray-700 flex items-center select-none cursor-pointer"
                onClick={() => {
                  if (!user) {
                    showToast(lang === "ko" ? "로그인 후 이용해주세요." : "Please login to use this feature.", "info");
                    router.push("/signin");
                  } else {
                    setQuery(prompt);
                  }
                }}
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
              onFocus={handleFocus}
              onKeyDown={handleKeyDown}
              placeholder={t.searchPlaceholder}
              rows={3}
              className="w-full resize-none bg-transparent border-none text-sm sm:text-base font-medium text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-0 leading-relaxed px-1"
            />

            {/* 하단 툴바 */}
            <div className="flex items-center justify-between pt-2 mt-1 border-t border-gray-100/80">
              <div className="text-xs text-gray-400 pl-1">
                {isSubmitting
                  ? lang === "ko"
                    ? "검색 중..."
                    : "Searching..."
                  : t.enterToSearch}
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs font-medium text-gray-400 select-none">
                  {query.length}/500
                </span>

                <button
                  type="button"
                  onClick={handleSend}
                  disabled={isSubmitting || query.trim().length === 0}
                  className="w-9 h-9 rounded-full bg-[#85132d] hover:bg-[#701025] disabled:bg-gray-200 disabled:cursor-not-allowed text-white flex items-center justify-center transition-all active:scale-90 shadow-sm cursor-pointer"
                  aria-label="전송"
                >
                  {isSubmitting ? (
                    <LoaderCircle className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4 -translate-x-0.5 translate-y-0.5" />
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
