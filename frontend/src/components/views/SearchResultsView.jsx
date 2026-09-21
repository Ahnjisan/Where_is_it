"use client";

import React, { useState, useMemo } from "react";
import {
  SlidersHorizontal,
  Calendar,
  MapPin,
  Heart,
  Send,
  Sparkles,
  PlusCircle,
  Check
} from "lucide-react";
import { I18N } from "@/lib/mockData";

export default function SearchResultsView({
  searchQuery = "",
  items = [],
  onSelectItem,
  onOpenFilter,
  filters = {},
  onToggleLike,
  onRegisterTracking,
  lang = "ko",
  showToast,
  onNewSearch
}) {
  const t = I18N[lang] || I18N.ko;

  const [sortMode, setSortMode] = useState("recommend"); // "recommend" | "latest"
  const [inlineQuery, setInlineQuery] = useState(searchQuery);

  // 필터링 및 정렬 처리
  const filteredItems = useMemo(() => {
    let list = [...items];

    // 카테고리 필터
    if (filters.category && filters.category !== "전체") {
      list = list.filter((item) => item.category === filters.category);
    }
    // 색상 필터
    if (filters.color && filters.color !== "전체") {
      list = list.filter((item) => item.color === filters.color);
    }
    // 지역 필터
    if (filters.region && filters.region !== "전체") {
      list = list.filter((item) => item.location.includes(filters.region));
    }

    // 정렬
    if (sortMode === "recommend") {
      list.sort((a, b) => b.matchRate - a.matchRate);
    } else {
      list.sort((a, b) => new Date(b.date) - new Date(a.date));
    }

    return list;
  }, [items, filters, sortMode]);

  const handleInlineSearch = (e) => {
    e.preventDefault();
    if (inlineQuery.trim()) {
      onNewSearch(inlineQuery.trim());
      showToast(
        lang === "ko" ? `'${inlineQuery}' 조건으로 검색했습니다.` : `Searched for '${inlineQuery}'`,
        "info"
      );
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] max-w-lg mx-auto w-full animate-in fade-in duration-300">
      {/* 상단 검색 쿼리 보정 바 (와이어프레임 04 상단 채팅 입력창) */}
      <div className="p-4 bg-white border-b border-gray-100 shadow-xs">
        <form onSubmit={handleInlineSearch} className="relative flex items-center">
          <input
            type="text"
            value={inlineQuery}
            onChange={(e) => setInlineQuery(e.target.value)}
            placeholder={t.searchPlaceholder}
            className="w-full h-12 pl-4 pr-11 rounded-2xl bg-gray-50 border border-gray-200 text-xs sm:text-sm font-medium text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all"
          />
          <button
            type="submit"
            className="absolute right-2 p-2 rounded-xl bg-[#85132d] text-white hover:bg-[#701025] transition-all active:scale-90"
            aria-label="재검색"
          >
            <Send className="w-3.5 h-3.5" />
          </button>
        </form>
      </div>

      {/* 정렬 탭 & 필터 보정 버튼 (와이어프레임 04) */}
      <div className="px-4 py-3 bg-white flex items-center justify-between border-b border-gray-100">
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setSortMode("recommend")}
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all ${
              sortMode === "recommend"
                ? "bg-[#85132d] text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200/80"
            }`}
          >
            {t.sortRecommend}
          </button>
          <button
            type="button"
            onClick={() => setSortMode("latest")}
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all ${
              sortMode === "latest"
                ? "bg-[#85132d] text-white shadow-xs"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200/80"
            }`}
          >
            {t.sortLatest}
          </button>
        </div>

        <button
          type="button"
          onClick={onOpenFilter}
          className="flex items-center gap-1 px-3 py-1.5 rounded-full bg-gray-50 border border-gray-200/90 hover:bg-gray-100 text-gray-700 text-xs font-semibold transition-all active:scale-95"
        >
          <SlidersHorizontal className="w-3.5 h-3.5 text-gray-500" />
          <span>{t.filterAdjust}</span>
        </button>
      </div>

      {/* 검색 결과 카운트 안내 문구 */}
      <div className="px-5 pt-3.5 pb-1">
        <p className="text-xs sm:text-sm font-medium text-gray-500">
          {t.resultCountPrefix}{" "}
          <strong className="text-gray-900 font-bold">{filteredItems.length}</strong>
          {t.resultCountSuffix}
        </p>
      </div>

      {/* 결과 카드 리스트 (와이어프레임 04) */}
      <div className="p-4 space-y-3 pb-24">
        {filteredItems.map((item) => (
          <div
            key={item.id}
            onClick={() => onSelectItem(item)}
            className="group relative bg-white rounded-2xl p-3.5 border border-gray-100 shadow-sm hover:shadow-md hover:border-gray-200 transition-all duration-200 cursor-pointer active:scale-[0.99] flex gap-3.5 items-center"
          >
            {/* 썸네일 이미지 */}
            <div className="w-20 h-20 sm:w-22 sm:h-22 rounded-xl overflow-hidden bg-gray-100 shrink-0 relative">
              <img
                src={item.images[0]}
                alt={item.name}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              />
            </div>

            {/* 카드 정보 */}
            <div className="flex-1 min-w-0 pr-6">
              <div className="flex items-center gap-2 mb-1">
                <h3 className="text-sm sm:text-base font-bold text-gray-900 truncate">
                  {item.name}
                </h3>
                {/* 일치율 뱃지 (민트/초록) */}
                <span className="shrink-0 inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-200/60">
                  {item.matchRate}% {t.matchSuffix}
                </span>
              </div>

              <div className="space-y-1 text-xs text-gray-500 font-medium">
                <div className="flex items-center gap-1.5 truncate">
                  <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                  <span>{item.date}</span>
                </div>
                <div className="flex items-center gap-1.5 truncate">
                  <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                  <span className="truncate">{item.location}</span>
                </div>
              </div>
            </div>

            {/* 관심(찜) 하트 버튼 */}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onToggleLike(item.id);
              }}
              className="absolute right-3.5 top-3.5 p-1.5 rounded-full hover:bg-gray-100 transition-all active:scale-90"
              aria-label="관심 등록"
            >
              <Heart
                className={`w-4 h-4 transition-colors ${
                  item.isLiked
                    ? "fill-rose-500 text-rose-500"
                    : "text-gray-300 hover:text-gray-500"
                }`}
              />
            </button>
          </div>
        ))}

        {filteredItems.length === 0 && (
          <div className="text-center py-12 bg-white rounded-2xl border border-gray-100 my-4 p-6">
            <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-3 text-gray-400">
              <Sparkles className="w-6 h-6" />
            </div>
            <p className="text-sm font-bold text-gray-800 mb-1">
              일치하는 습득물이 없습니다.
            </p>
            <p className="text-xs text-gray-500 mb-4">
              필터 조건을 완화하거나 아래에서 추적 등록을 신청해 보세요.
            </p>
            <button
              type="button"
              onClick={onOpenFilter}
              className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-bold text-gray-700 transition-colors"
            >
              필터 초기화
            </button>
          </div>
        )}
      </div>

      {/* 하단 고정: '내 분실물이 없나요? 추적 등록하기' (와이어프레임 04 하단 액션) */}
      <div className="sticky bottom-14 left-0 right-0 p-4 bg-gradient-to-t from-[#f8f9fa] via-[#f8f9fa]/95 to-transparent z-20">
        <div className="max-w-md mx-auto">
          <button
            type="button"
            onClick={() => onRegisterTracking(searchQuery || "지갑 (검정색)")}
            className="w-full h-13 rounded-2xl bg-white hover:bg-[#fff1f3] border-2 border-[#85132d] text-[#85132d] font-bold text-xs sm:text-sm shadow-md transition-all active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
          >
            <PlusCircle className="w-4 h-4" />
            <span>{t.trackRegisterCta}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
