"use client";

import React, { useState, useEffect } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Send,
  Calendar,
  RotateCcw,
  Sparkles,
  Filter,
  ChevronDown,
  Check
} from "lucide-react";
import { CATEGORIES, COLORS, I18N } from "@/lib/mockData";

// 커스텀 프리미엄 캘린더 피커 컴포넌트
function CustomDatePicker({
  lostDate,
  onSelectDate,
  isOpen,
  onToggle,
  onClose,
  t
}) {
  // 기준 연월 (기존 lostDate가 있으면 그 날짜, 없으면 2025년 9월 14일 기준)
  const [currentMonth, setCurrentMonth] = useState(() => {
    if (lostDate) {
      const d = new Date(lostDate);
      return new Date(d.getFullYear(), d.getMonth(), 1);
    }
    return new Date(2025, 8, 1);
  });

  // lostDate가 변경되면 달력 월도 동기화
  useEffect(() => {
    if (lostDate) {
      const d = new Date(lostDate);
      if (!isNaN(d.getTime())) {
        setCurrentMonth(new Date(d.getFullYear(), d.getMonth(), 1));
      }
    }
  }, [lostDate]);

  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth(); // 0 ~ 11

  const handlePrevMonth = (e) => {
    e.stopPropagation();
    setCurrentMonth(new Date(year, month - 1, 1));
  };

  const handleNextMonth = (e) => {
    e.stopPropagation();
    setCurrentMonth(new Date(year, month + 1, 1));
  };

  const firstDayOfWeek = new Date(year, month, 1).getDay(); // 0(일) ~ 6(토)
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const handleDayClick = (day) => {
    const formattedMonth = String(month + 1).padStart(2, "0");
    const formattedDay = String(day).padStart(2, "0");
    const dateString = `${year}-${formattedMonth}-${formattedDay}`;
    onSelectDate(dateString);
    onClose();
  };

  const formatDisplayDate = (dStr) => {
    if (!dStr) return "날짜를 선택해 주세요 (전체 날짜)";
    try {
      const d = new Date(dStr);
      if (isNaN(d.getTime())) return dStr;
      const weekDays = ["일", "월", "화", "수", "목", "금", "토"];
      const y = d.getFullYear();
      const m = d.getMonth() + 1;
      const day = d.getDate();
      const w = weekDays[d.getDay()];
      return `${y}년 ${m}월 ${day}일 (${w})`;
    } catch {
      return dStr;
    }
  };

  const applyPreset = (daysAgo) => {
    const base = new Date(2025, 8, 15);
    base.setDate(base.getDate() - daysAgo);
    const y = base.getFullYear();
    const m = String(base.getMonth() + 1).padStart(2, "0");
    const d = String(base.getDate()).padStart(2, "0");
    onSelectDate(`${y}-${m}-${d}`);
    onClose();
  };

  return (
    <div className="pt-1">
      <div className="flex items-center justify-between mb-1.5">
        <label className="text-[11px] font-semibold text-gray-600">
          {t.lostDate || "분실한 날짜"}
        </label>
        {lostDate && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onSelectDate("");
            }}
            className="text-[10px] text-gray-400 hover:text-gray-700 underline cursor-pointer"
          >
            날짜 선택 해제
          </button>
        )}
      </div>

      {/* 날짜 선택 트리거 버튼 */}
      <button
        type="button"
        onClick={onToggle}
        className={`w-full h-11 px-3.5 rounded-xl border text-xs font-semibold flex items-center justify-between transition-all cursor-pointer ${
          isOpen
            ? "border-[#85132d] ring-2 ring-[#85132d]/15 bg-white text-[#85132d] shadow-2xs"
            : lostDate
            ? "bg-white border-[#85132d]/40 text-gray-900 shadow-2xs"
            : "bg-gray-50 border-gray-200 text-gray-500 hover:bg-gray-100/80"
        }`}
      >
        <div className="flex items-center gap-2 truncate">
          <Calendar
            className={`w-4 h-4 shrink-0 ${lostDate ? "text-[#85132d]" : "text-gray-400"}`}
          />
          <span className="truncate">{formatDisplayDate(lostDate)}</span>
        </div>
        <ChevronDown
          className={`w-4 h-4 transition-transform duration-200 shrink-0 ${
            isOpen ? "rotate-180 text-[#85132d]" : "text-gray-500"
          }`}
        />
      </button>

      {/* 인라인 확장 캘린더 패널 */}
      {isOpen && (
        <div className="mt-2.5 bg-white border border-gray-200 rounded-2xl p-4 shadow-sm animate-in fade-in slide-in-from-top-2 duration-200">
          {/* 빠른 프리셋 칩 바 */}
          <div className="flex items-center gap-1.5 mb-3.5 pb-3 border-b border-gray-100 overflow-x-auto scrollbar-none">
            <span className="text-[11px] font-bold text-gray-400 shrink-0 mr-0.5">
              빠른 선택
            </span>
            <button
              type="button"
              onClick={() => applyPreset(0)}
              className="px-2.5 py-1 rounded-full bg-gray-100 hover:bg-[#85132d]/10 hover:text-[#85132d] text-[11px] font-medium text-gray-700 shrink-0 cursor-pointer transition-colors"
            >
              오늘
            </button>
            <button
              type="button"
              onClick={() => applyPreset(1)}
              className="px-2.5 py-1 rounded-full bg-gray-100 hover:bg-[#85132d]/10 hover:text-[#85132d] text-[11px] font-medium text-gray-700 shrink-0 cursor-pointer transition-colors"
            >
              어제
            </button>
            <button
              type="button"
              onClick={() => applyPreset(3)}
              className="px-2.5 py-1 rounded-full bg-gray-100 hover:bg-[#85132d]/10 hover:text-[#85132d] text-[11px] font-medium text-gray-700 shrink-0 cursor-pointer transition-colors"
            >
              3일 전
            </button>
            <button
              type="button"
              onClick={() => applyPreset(7)}
              className="px-2.5 py-1 rounded-full bg-gray-100 hover:bg-[#85132d]/10 hover:text-[#85132d] text-[11px] font-medium text-gray-700 shrink-0 cursor-pointer transition-colors"
            >
              1주일 전
            </button>
          </div>

          {/* 연월 헤더 및 이전/다음 달 이동 */}
          <div className="flex items-center justify-between mb-3 px-1">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors cursor-pointer"
              aria-label="이전 달"
            >
              <ChevronLeft className="w-4 h-4 stroke-[2.2]" />
            </button>
            <span className="text-sm font-bold text-gray-900 tracking-tight">
              {year}년 {month + 1}월
            </span>
            <button
              type="button"
              onClick={handleNextMonth}
              className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors cursor-pointer"
              aria-label="다음 달"
            >
              <ChevronRight className="w-4 h-4 stroke-[2.2]" />
            </button>
          </div>

          {/* 요일 헤더 */}
          <div className="grid grid-cols-7 gap-1 text-center mb-1.5 text-[11px] font-semibold text-gray-400">
            <span className="text-rose-500">일</span>
            <span>월</span>
            <span>화</span>
            <span>수</span>
            <span>목</span>
            <span>금</span>
            <span className="text-blue-500">토</span>
          </div>

          {/* 날짜 그리드 */}
          <div className="grid grid-cols-7 gap-1 text-center">
            {Array.from({ length: firstDayOfWeek }).map((_, i) => (
              <div key={`empty-${i}`} className="h-8 w-8" />
            ))}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1;
              const formattedMonth = String(month + 1).padStart(2, "0");
              const formattedDay = String(day).padStart(2, "0");
              const thisDateStr = `${year}-${formattedMonth}-${formattedDay}`;
              const isSelected = lostDate === thisDateStr;
              const dayOfWeek = (firstDayOfWeek + i) % 7;

              return (
                <button
                  key={thisDateStr}
                  type="button"
                  onClick={() => handleDayClick(day)}
                  className={`h-8 w-8 mx-auto rounded-xl text-xs font-semibold flex items-center justify-center transition-all cursor-pointer ${
                    isSelected
                      ? "bg-[#85132d] text-white shadow-sm font-bold"
                      : dayOfWeek === 0
                      ? "text-rose-500 hover:bg-rose-50"
                      : dayOfWeek === 6
                      ? "text-blue-500 hover:bg-blue-50"
                      : "text-gray-700 hover:bg-gray-100"
                  }`}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

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
  const [category, setCategory] = useState(currentFilters?.category || "전체");
  const [color, setColor] = useState(currentFilters?.color || "전체");
  const [lostDate, setLostDate] = useState(currentFilters?.lostDate || "");
  const [openSection, setOpenSection] = useState(null); // "category" | "color" | "date" | null

  // 모달이 열릴 때 최신 필터 상태 동기화
  useEffect(() => {
    if (isOpen) {
      setCategory(currentFilters?.category || "전체");
      setColor(currentFilters?.color || "전체");
      setLostDate(currentFilters?.lostDate || "");
      setOpenSection(null);
    }
  }, [isOpen, currentFilters]);

  if (!isOpen) return null;

  const handleApplyNl = (e) => {
    e?.preventDefault();
    if (!nlQuery.trim()) {
      showToast(
        lang === "ko"
          ? "보정할 검색 내용을 입력해 주세요."
          : "Please enter your search details.",
        "warning"
      );
      return;
    }
    onApplyFilters({
      nlQuery: nlQuery.trim()
    });
    showToast(
      lang === "ko"
        ? "자연어 검색 조건이 보정되었습니다."
        : "Search condition updated with natural language.",
      "success"
    );
    onClose();
  };

  const handleApplyManual = () => {
    onApplyFilters({
      category,
      color,
      lostDate
    });
    showToast(
      lang === "ko"
        ? "상세 필터 조건이 적용되었습니다."
        : "Filter settings updated successfully.",
      "success"
    );
    onClose();
  };

  const handleReset = () => {
    if (activeTab === "nl") {
      setNlQuery("");
    } else {
      setCategory("전체");
      setColor("전체");
      setLostDate("");
      setOpenSection(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-end sm:items-center justify-center animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-md max-h-[90vh] sm:rounded-3xl rounded-t-3xl shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom duration-300">
        {/* 모달 상단 헤더 */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 -ml-1 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
              aria-label="닫기"
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
            className="flex items-center gap-1 text-xs font-semibold text-gray-500 hover:text-gray-900 transition-colors p-1 cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>초기화</span>
          </button>
        </div>

        {/* 상단 탭 (자연어 보정 vs 상세 필터) */}
        <div className="p-4 pb-2">
          <div className="flex p-1 bg-gray-100/90 rounded-2xl">
            <button
              type="button"
              onClick={() => {
                setActiveTab("nl");
                setOpenSection(null);
              }}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeTab === "nl"
                  ? "bg-[#85132d] text-white shadow-sm"
                  : "text-gray-600 hover:text-gray-900"
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{t.tabNaturalLanguage}</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setActiveTab("manual");
                setOpenSection(null);
              }}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeTab === "manual"
                  ? "bg-[#85132d] text-white shadow-sm"
                  : "text-gray-600 hover:text-gray-900"
              }`}
            >
              <Filter className="w-3.5 h-3.5" />
              <span>{t.tabDetailFilter}</span>
            </button>
          </div>
        </div>

        {/* 탭 본문 영역 */}
        <div className="flex-1 overflow-y-auto px-5 py-4">
          {activeTab === "nl" ? (
            /* 1. 자연어 보정 탭 본문 */
            <div className="space-y-4">
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-gray-800 mb-1">
                  {t.nlPromptHeader}
                </h3>
                <p className="text-[12px] text-gray-400">
                  {lang === "ko"
                    ? "추가하고 싶은 단서나 상황을 자연스럽게 입력해 보세요."
                    : "Describe more clues or context in your own words."}
                </p>
              </div>

              <div className="relative">
                <textarea
                  value={nlQuery}
                  onChange={(e) => setNlQuery(e.target.value)}
                  placeholder={t.nlPromptPlaceholder}
                  rows={4}
                  className="w-full p-4 pr-11 rounded-2xl bg-gray-50 border border-gray-200 text-xs sm:text-sm text-gray-900 placeholder:text-gray-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d] transition-all resize-none leading-relaxed"
                />
                <button
                  type="button"
                  onClick={handleApplyNl}
                  disabled={!nlQuery.trim()}
                  className="absolute right-3 bottom-3 p-2 rounded-xl bg-[#85132d] text-white disabled:bg-gray-200 disabled:text-gray-400 transition-colors cursor-pointer active:scale-95"
                  aria-label="자연어 보정 적용"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            /* 2. 상세 필터 탭 본문 */
            <div className="space-y-4 pb-8">
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-gray-800 mb-1">
                  {t.manualFilterHeader}
                </h3>
                <p className="text-[12px] text-gray-400">
                  {lang === "ko"
                    ? "물품 분류, 색상, 분실일을 직접 선택하여 좁혀보세요."
                    : "Select category, color, and date lost manually."}
                </p>
              </div>

              {/* 물품 분류 & 색상 2분할 버튼 */}
              <div className="grid grid-cols-2 gap-3">
                {/* 물품 분류 트리거 */}
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-1.5">
                    {t.categoryLabel || "물품 분류"}
                  </label>
                  <button
                    type="button"
                    onClick={() =>
                      setOpenSection((prev) => (prev === "category" ? null : "category"))
                    }
                    className={`w-full h-11 px-3 rounded-xl border text-xs font-semibold flex items-center justify-between transition-all cursor-pointer ${
                      openSection === "category"
                        ? "border-[#85132d] ring-2 ring-[#85132d]/15 bg-white text-[#85132d] shadow-2xs"
                        : "bg-gray-50 border-gray-200 text-gray-800 hover:bg-gray-100/80"
                    }`}
                  >
                    <span className="truncate">{category}</span>
                    <ChevronDown
                      className={`w-4 h-4 transition-transform duration-200 shrink-0 ${
                        openSection === "category" ? "rotate-180 text-[#85132d]" : "text-gray-500"
                      }`}
                    />
                  </button>
                </div>

                {/* 색상 트리거 */}
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-1.5">
                    {t.colorLabel || "색상"}
                  </label>
                  <button
                    type="button"
                    onClick={() =>
                      setOpenSection((prev) => (prev === "color" ? null : "color"))
                    }
                    className={`w-full h-11 px-3 rounded-xl border text-xs font-semibold flex items-center justify-between transition-all cursor-pointer ${
                      openSection === "color"
                        ? "border-[#85132d] ring-2 ring-[#85132d]/15 bg-white text-[#85132d] shadow-2xs"
                        : "bg-gray-50 border-gray-200 text-gray-800 hover:bg-gray-100/80"
                    }`}
                  >
                    <span className="truncate">{color}</span>
                    <ChevronDown
                      className={`w-4 h-4 transition-transform duration-200 shrink-0 ${
                        openSection === "color" ? "rotate-180 text-[#85132d]" : "text-gray-500"
                      }`}
                    />
                  </button>
                </div>
              </div>

              {/* 인라인 확장: 물품 분류 선택 목록 */}
              {openSection === "category" && (
                <div className="bg-gray-50/90 border border-gray-200 rounded-2xl p-3 animate-in fade-in slide-in-from-top-2 duration-200 shadow-2xs">
                  <div className="flex items-center justify-between px-1.5 pb-2 mb-2 border-b border-gray-200/70">
                    <span className="text-xs font-bold text-gray-800">
                      물품 분류 선택 (19종)
                    </span>
                    <button
                      type="button"
                      onClick={() => setOpenSection(null)}
                      className="text-[11px] text-gray-400 hover:text-gray-700 font-medium px-1.5 py-0.5 rounded cursor-pointer"
                    >
                      접기 ✕
                    </button>
                  </div>
                  <div className="max-h-52 overflow-y-auto space-y-1 pr-1 scrollbar-thin">
                    {CATEGORIES.map((cat) => {
                      const isSelected = cat === category;
                      return (
                        <button
                          key={cat}
                          type="button"
                          onClick={() => {
                            setCategory(cat);
                            setOpenSection(null);
                          }}
                          className={`w-full px-3 py-2.5 rounded-xl text-xs text-left flex items-center justify-between transition-all cursor-pointer ${
                            isSelected
                              ? "bg-[#85132d] text-white font-bold shadow-xs"
                              : "text-gray-700 hover:bg-white hover:text-gray-900"
                          }`}
                        >
                          <span>{cat}</span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-white" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* 인라인 확장: 색상 선택 목록 */}
              {openSection === "color" && (
                <div className="bg-gray-50/90 border border-gray-200 rounded-2xl p-3 animate-in fade-in slide-in-from-top-2 duration-200 shadow-2xs">
                  <div className="flex items-center justify-between px-1.5 pb-2 mb-2 border-b border-gray-200/70">
                    <span className="text-xs font-bold text-gray-800">색상 선택</span>
                    <button
                      type="button"
                      onClick={() => setOpenSection(null)}
                      className="text-[11px] text-gray-400 hover:text-gray-700 font-medium px-1.5 py-0.5 rounded cursor-pointer"
                    >
                      접기 ✕
                    </button>
                  </div>
                  <div className="max-h-52 overflow-y-auto space-y-1 pr-1 scrollbar-thin">
                    {COLORS.map((clr) => {
                      const isSelected = clr === color;
                      return (
                        <button
                          key={clr}
                          type="button"
                          onClick={() => {
                            setColor(clr);
                            setOpenSection(null);
                          }}
                          className={`w-full px-3 py-2.5 rounded-xl text-xs text-left flex items-center justify-between transition-all cursor-pointer ${
                            isSelected
                              ? "bg-[#85132d] text-white font-bold shadow-xs"
                              : "text-gray-700 hover:bg-white hover:text-gray-900"
                          }`}
                        >
                          <span>{clr}</span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-white" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* 커스텀 프리미엄 캘린더 Datepicker (투박한 네이티브 date input 완전 교체) */}
              <CustomDatePicker
                lostDate={lostDate}
                onSelectDate={setLostDate}
                isOpen={openSection === "date"}
                onToggle={() =>
                  setOpenSection((prev) => (prev === "date" ? null : "date"))
                }
                onClose={() => setOpenSection(null)}
                t={t}
              />
            </div>
          )}
        </div>

        {/* 모달 하단 고정 버튼 */}
        <div className="p-4 border-t border-gray-100 bg-gray-50/90 z-10 shrink-0">
          <button
            type="button"
            onClick={activeTab === "nl" ? handleApplyNl : handleApplyManual}
            disabled={activeTab === "nl" && !nlQuery.trim()}
            className="w-full h-12 rounded-2xl bg-[#85132d] hover:bg-[#701025] disabled:bg-gray-200 disabled:text-gray-400 text-white font-bold text-sm shadow-md transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer"
          >
            {activeTab === "nl"
              ? t.nlApplyBtn || "자연어로 조건 보정하기"
              : t.applyFilterBtn || "필터 적용하기"}
          </button>
        </div>
      </div>
    </div>
  );
}
