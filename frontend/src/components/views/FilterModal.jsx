"use client";

import React, { useState } from "react";
import { ChevronLeft, Send, Calendar, Check, RotateCcw } from "lucide-react";
import { CATEGORIES, COLORS, REGIONS, I18N } from "@/lib/mockData";

export default function FilterModal({
  isOpen,
  onClose,
  currentFilters,
  onApplyFilters,
  lang = "ko",
  showToast
}) {
  const t = I18N[lang] || I18N.ko;

  const [activeTab, setActiveTab] = useState("nl"); // "nl" (자연어 보정) | "manual" (상세 필터)
  const [nlQuery, setNlQuery] = useState("");
  const [category, setCategory] = useState(currentFilters.category || "전체");
  const [color, setColor] = useState(currentFilters.color || "전체");
  const [region, setRegion] = useState(currentFilters.region || "전체");
  const [startDate, setStartDate] = useState(currentFilters.startDate || "2025-09-10");
  const [endDate, setEndDate] = useState(currentFilters.endDate || "2025-09-15");

  if (!isOpen) return null;

  const handleApply = () => {
    onApplyFilters({
      nlQuery: nlQuery.trim(),
      category,
      color,
      region,
      startDate,
      endDate
    });
    showToast(
      lang === "ko"
        ? "검색 조건이 보정되어 새로고침되었습니다."
        : "Search criteria updated successfully.",
      "success"
    );
    onClose();
  };

  const handleReset = () => {
    setNlQuery("");
    setCategory("전체");
    setColor("전체");
    setRegion("전체");
    setStartDate("2025-09-10");
    setEndDate("2025-09-15");
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-end sm:items-center justify-center animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-md max-h-[92vh] sm:rounded-3xl rounded-t-3xl shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom duration-300">
        {/* 모달 상단 헤더 */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 -ml-1 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95"
            >
              <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
            </button>
            <h2 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
              {t.filterTitle}
            </h2>
          </div>
          <button
            type="button"
            onClick={handleReset}
            className="flex items-center gap-1 text-xs font-semibold text-gray-500 hover:text-gray-900 transition-colors p-1"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>초기화</span>
          </button>
        </div>

        {/* 상단 탭 (와이어프레임 06) */}
        <div className="p-4 pb-2">
          <div className="flex p-1 bg-gray-100 rounded-2xl">
            <button
              type="button"
              onClick={() => setActiveTab("nl")}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === "nl"
                  ? "bg-[#85132d] text-white shadow-sm"
                  : "text-gray-600 hover:text-gray-900"
              }`}
            >
              {t.tabNaturalLanguage}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("manual")}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === "manual"
                  ? "bg-[#85132d] text-white shadow-sm"
                  : "text-gray-600 hover:text-gray-900"
              }`}
            >
              {t.tabDetailFilter}
            </button>
          </div>
        </div>

        {/* 탭 본문 영역 */}
        <div className="flex-1 overflow-y-auto px-5 py-3 space-y-5">
          {/* 자연어 보정 섹션 */}
          <div className="space-y-2">
            <h3 className="text-xs sm:text-sm font-bold text-gray-800">
              {t.nlPromptHeader}
            </h3>
            <div className="relative">
              <textarea
                value={nlQuery}
                onChange={(e) => setNlQuery(e.target.value)}
                placeholder={t.nlPromptPlaceholder}
                rows={3}
                className="w-full p-3.5 pr-10 rounded-2xl bg-gray-50 border border-gray-200 text-xs sm:text-sm text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all resize-none"
              />
              <button
                type="button"
                onClick={handleApply}
                disabled={!nlQuery.trim()}
                className="absolute right-3 bottom-3 p-1.5 rounded-full bg-[#85132d] text-white disabled:bg-gray-200 disabled:text-gray-400 transition-colors"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          <div className="border-t border-gray-100 pt-3 space-y-4">
            <h3 className="text-xs sm:text-sm font-bold text-gray-800">
              {t.manualFilterHeader}
            </h3>

            {/* 분류, 색상, 지역 3분할 셀렉트 */}
            <div className="grid grid-cols-3 gap-2.5">
              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1.5">
                  물품 분류
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full h-11 px-2.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-semibold text-gray-800 focus:outline-none focus:border-[#85132d]"
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1.5">
                  색상
                </label>
                <select
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="w-full h-11 px-2.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-semibold text-gray-800 focus:outline-none focus:border-[#85132d]"
                >
                  {COLORS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1.5">
                  지역
                </label>
                <select
                  value={region}
                  onChange={(e) => setRegion(e.target.value)}
                  className="w-full h-11 px-2.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-semibold text-gray-800 focus:outline-none focus:border-[#85132d]"
                >
                  {REGIONS.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* 날짜 범위 설정 */}
            <div className="grid grid-cols-2 gap-3 pt-1">
              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1.5">
                  {t.startDate}
                </label>
                <div className="relative">
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full h-11 px-3 rounded-xl bg-gray-50 border border-gray-200 text-xs font-medium text-gray-800 focus:outline-none focus:border-[#85132d]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1.5">
                  {t.endDate}
                </label>
                <div className="relative">
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="w-full h-11 px-3 rounded-xl bg-gray-50 border border-gray-200 text-xs font-medium text-gray-800 focus:outline-none focus:border-[#85132d]"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 모달 하단 버튼 (와이어프레임 06) */}
        <div className="p-4 border-t border-gray-100 bg-gray-50/60">
          <button
            type="button"
            onClick={handleApply}
            className="w-full h-13 rounded-2xl bg-[#85132d] hover:bg-[#701025] text-white font-bold text-sm shadow-md transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer"
          >
            {t.applyFilterBtn}
          </button>
        </div>
      </div>
    </div>
  );
}
