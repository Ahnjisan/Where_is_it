"use client";

import React from "react";
import { ChevronLeft, Heart, Share2, User } from "lucide-react";
import BrandLogo from "@/components/common/BrandLogo";
import LanguageSelector from "@/components/common/LanguageSelector";
import { I18N } from "@/lib/mockData";

export default function TopBar({
  title = "",
  showBack = false,
  onBack,
  showLogo = false,
  showLang = true,
  lang = "ko",
  onChangeLang,
  rightActions = null,
  isLiked = false,
  onToggleLike,
  onShare,
  onProfileClick
}) {
  const t = I18N[lang] || I18N.ko;

  return (
    <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100/80 transition-all">
      <div className="max-w-md mx-auto h-14 px-4 flex items-center justify-between">
        {/* 좌측 영역 */}
        <div className="flex items-center gap-2">
          {showBack && (
            <button
              type="button"
              onClick={onBack}
              className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95"
              aria-label="뒤로가기"
            >
              <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
            </button>
          )}

          {showLogo && (
            <div onClick={onBack} className="cursor-pointer">
              <BrandLogo size="sm" lang={lang} />
            </div>
          )}

          {title && !showLogo && (
            <h1 className="text-base sm:text-lg font-bold text-[#191f28] tracking-tight">
              {title}
            </h1>
          )}
        </div>

        {/* 우측 영역 */}
        <div className="flex items-center gap-2">
          {showLang && (
            <LanguageSelector
              currentLang={lang}
              onChangeLang={onChangeLang}
              variant="badge"
            />
          )}

          {/* 상세화면 액션 버튼: 하트 & 공유 */}
          {onToggleLike && (
            <button
              type="button"
              onClick={onToggleLike}
              className="p-2 rounded-full hover:bg-gray-100 transition-all active:scale-90"
              aria-label="관심 저장"
            >
              <Heart
                className={`w-5 h-5 transition-colors ${
                  isLiked
                    ? "fill-rose-500 text-rose-500"
                    : "text-gray-600 hover:text-gray-900"
                }`}
              />
            </button>
          )}

          {onShare && (
            <button
              type="button"
              onClick={onShare}
              className="p-2 rounded-full hover:bg-gray-100 text-gray-600 hover:text-gray-900 transition-all active:scale-90"
              aria-label="공유하기"
            >
              <Share2 className="w-5 h-5" />
            </button>
          )}

          {onProfileClick && (
            <button
              type="button"
              onClick={onProfileClick}
              className="p-2 rounded-full hover:bg-gray-100 text-gray-600 hover:text-gray-900 transition-all active:scale-95"
              aria-label="프로필"
            >
              <User className="w-5 h-5" />
            </button>
          )}

          {rightActions}
        </div>
      </div>
    </header>
  );
}
