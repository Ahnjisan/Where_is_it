"use client";

import React, { useState, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Image as ImageIcon,
  Paperclip,
  Send,
  X,
  User
} from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import LanguageSelector from "@/components/common/LanguageSelector";
import { EXAMPLE_PROMPTS } from "@/lib/mockData";
import { useApp } from "@/context/AppContext";

export default function HomePage() {
  const router = useRouter();
  const { lang, setLang, t, showToast } = useApp();

  const [query, setQuery] = useState("");
  const [attachedImage, setAttachedImage] = useState(null);
  const fileInputRef = useRef(null);

  const handleSend = () => {
    if (!query.trim() && !attachedImage) {
      showToast(
        lang === "ko"
          ? "분실물에 대한 내용을 입력해 주세요."
          : "Please describe your lost item.",
        "info"
      );
      return;
    }

    const searchQueryText = query.trim() || (lang === "ko" ? "지갑" : "Wallet");
    router.push(`/search?q=${encodeURIComponent(searchQueryText)}`);
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChipClick = (promptText) => {
    router.push(`/search?q=${encodeURIComponent(promptText)}`);
  };

  const handleImageUpload = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      setAttachedImage(imageUrl);
      showToast(
        lang === "ko" ? "이미지가 첨부되었습니다." : "Image attached.",
        "info"
      );
    }
  };

  return (
    <div className="flex-1 flex flex-col justify-between animate-in fade-in duration-200">
      {/* 상단 네비게이션 헤더 (와이어프레임 03) */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <Link href="/" className="cursor-pointer">
          <BrandLogo size="sm" lang={lang} />
        </Link>

        <div className="flex items-center gap-2">
          <LanguageSelector
            currentLang={lang}
            onChangeLang={setLang}
            variant="badge"
          />
          <Link
            href="/my"
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
          <p className="text-sm sm:text-base text-gray-500 font-medium leading-relaxed">
            {t.mainSub}
          </p>
        </div>

        {/* 예시 질문 칩 섹션 */}
        <div className="my-6 space-y-3">
          <span className="text-xs font-semibold text-gray-400 block tracking-wide">
            {t.examplePromptLabel}
          </span>
          <div className="flex flex-col gap-2.5">
            {EXAMPLE_PROMPTS.map((prompt, index) => (
              <button
                key={index}
                type="button"
                onClick={() => handleChipClick(prompt)}
                className="w-full text-left px-4 py-3.5 rounded-2xl bg-white hover:bg-[#fff1f3] border border-gray-200/80 hover:border-[#fecdd3] text-xs sm:text-sm font-medium text-gray-700 hover:text-[#85132d] shadow-2xs transition-all duration-200 active:scale-[0.99] flex items-center justify-between group cursor-pointer"
              >
                <span className="truncate">&ldquo;{prompt}&rdquo;</span>
                <span className="text-gray-300 group-hover:text-[#85132d] text-xs transition-colors">
                  →
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* 대화형 검색 입력창 (ChatGPT / Gemini 스타일) */}
        <div className="mt-auto pt-4">
          <div className="bg-white rounded-3xl p-3.5 shadow-lg shadow-black/[0.04] border border-gray-200/90 focus-within:border-[#85132d] focus-within:ring-2 focus-within:ring-[#85132d]/15 transition-all">
            {/* 첨부 이미지 프리뷰 */}
            {attachedImage && (
              <div className="relative inline-block mb-2 ml-1">
                <img
                  src={attachedImage}
                  alt="첨부 이미지"
                  className="w-16 h-16 object-cover rounded-xl border border-gray-200"
                />
                <button
                  type="button"
                  onClick={() => setAttachedImage(null)}
                  className="absolute -top-1.5 -right-1.5 w-5 h-5 bg-gray-900 text-white rounded-full flex items-center justify-center text-xs shadow hover:bg-rose-600 transition-colors"
                >
                  <X className="w-3 h-3" />
                </button>
              </div>
            )}

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
              <div className="flex items-center gap-1">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleImageUpload}
                  accept="image/*"
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all active:scale-90 cursor-pointer"
                  title="이미지 첨부"
                >
                  <ImageIcon className="w-5 h-5" />
                </button>

                <button
                  type="button"
                  onClick={() =>
                    showToast(
                      lang === "ko"
                        ? "파일 첨부 기능이 선택되었습니다."
                        : "File attachment chosen.",
                      "info"
                    )
                  }
                  className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all active:scale-90 cursor-pointer"
                  title="파일 첨부"
                >
                  <Paperclip className="w-5 h-5" />
                </button>
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs font-medium text-gray-400 select-none">
                  {query.length}/500
                </span>

                <button
                  type="button"
                  onClick={handleSend}
                  disabled={!query.trim() && !attachedImage}
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
