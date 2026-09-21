"use client";

import React, { useState, useRef } from "react";
import { Image as ImageIcon, Paperclip, Send, X } from "lucide-react";
import { I18N, EXAMPLE_PROMPTS } from "@/lib/mockData";

export default function MainSearchView({
  onSearch,
  lang = "ko",
  showToast
}) {
  const t = I18N[lang] || I18N.ko;
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
    onSearch(query.trim() || (lang === "ko" ? "첨부된 분실물 사진" : "Attached item photo"));
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChipClick = (promptText) => {
    setQuery(promptText);
    onSearch(promptText);
  };

  const handleImageUpload = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      setAttachedImage(imageUrl);
      showToast(
        lang === "ko" ? "이미지가 첨부되었습니다." : "Image attached successfully.",
        "info"
      );
    }
  };

  return (
    <div className="flex-1 flex flex-col justify-between px-5 py-6 max-w-lg mx-auto w-full animate-in fade-in duration-300">
      {/* 중앙 타이틀 & 서브타이틀 */}
      <div className="mt-4 sm:mt-8 space-y-3">
        <h2 className="text-2xl sm:text-3xl font-extrabold text-[#191f28] tracking-tight leading-tight">
          {t.mainQuestion}
        </h2>
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
              className="w-full text-left px-4 py-3 rounded-2xl bg-white hover:bg-[#fff1f3] border border-gray-200/80 hover:border-[#fecdd3] text-xs sm:text-sm font-medium text-gray-700 hover:text-[#85132d] shadow-sm transition-all duration-200 active:scale-[0.99] flex items-center justify-between group"
            >
              <span className="truncate">"{prompt}"</span>
              <span className="text-gray-300 group-hover:text-[#85132d] text-xs transition-colors">
                →
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* 대화형 검색 입력창 (ChatGPT / Gemini / Toss 감성) */}
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
              {/* 이미지 첨부 input 숨김 & 버튼 */}
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
                className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all active:scale-90"
                title="이미지 첨부"
              >
                <ImageIcon className="w-5 h-5" />
              </button>

              <button
                type="button"
                onClick={() =>
                  showToast(
                    lang === "ko"
                      ? "파일 첨부 기능이 활성화되었습니다."
                      : "File attachment clicked.",
                    "info"
                  )
                }
                className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all active:scale-90"
                title="파일 첨부"
              >
                <Paperclip className="w-5 h-5" />
              </button>
            </div>

            <div className="flex items-center gap-3">
              {/* 글자수 카운터 */}
              <span className="text-xs font-medium text-gray-400 select-none">
                {query.length}/500
              </span>

              {/* 전송 버튼 */}
              <button
                type="button"
                onClick={handleSend}
                disabled={!query.trim() && !attachedImage}
                className="w-9 h-9 rounded-full bg-[#85132d] hover:bg-[#701025] disabled:bg-gray-200 disabled:cursor-not-allowed text-white flex items-center justify-center transition-all active:scale-90 shadow-sm"
                aria-label="전송"
              >
                <Send className="w-4 h-4 -translate-x-0.5 translate-y-0.5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
