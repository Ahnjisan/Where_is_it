"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  ExternalLink,
  MessageSquareQuote,
  Search,
  AlertCircle
} from "lucide-react";
import LanguageSelector from "@/components/common/LanguageSelector";
import { useApp } from "@/context/AppContext";

export default function TrackingPage() {
  const router = useRouter();
  const { lang, setLang, t, trackingData, stopTracking } = useApp();

  const [activeTab, setActiveTab] = useState("active");
  const [itemToConfirm, setItemToConfirm] = useState(null);

  const activeList = trackingData.active || [];
  const completedList = trackingData.completed || [];
  const currentList = activeTab === "active" ? activeList : completedList;

  const handleCandidateClick = (item) => {
    // 키워드 기반으로 /search 검색 페이지 연동
    const query = item.title || item.name || item.category;
    router.push(`/search?q=${encodeURIComponent(query)}`);
  };

  const getStatusText = (status) => {
    if (lang !== "en") return status || t.statusEnded;
    if (status === "기한 만료") return t.statusExpired;
    return t.statusEnded;
  };

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-24 min-h-screen">
      {/* 상단 네비게이션 헤더 */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
            aria-label={lang === "en" ? "Go back" : "뒤로가기"}
          >
            <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
          </button>
          <h1 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
            {t.trackingTitle}
          </h1>
        </div>

        <LanguageSelector
          currentLang={lang}
          onChangeLang={setLang}
          variant="badge"
        />
      </header>

      {/* 상단 탭: [진행 중 (N)] / [종료됨 (N)] */}
      <div className="p-4 bg-white border-b border-gray-100">
        <div className="flex p-1 bg-gray-100/80 rounded-2xl">
          <button
            type="button"
            onClick={() => setActiveTab("active")}
            className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all cursor-pointer ${
              activeTab === "active"
                ? "bg-[#85132d] text-white shadow-xs"
                : "text-gray-600 hover:text-gray-900"
            }`}
          >
            {t.tabActive} ({activeList.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("completed")}
            className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all cursor-pointer ${
              activeTab === "completed"
                ? "bg-[#85132d] text-white shadow-xs"
                : "text-gray-600 hover:text-gray-900"
            }`}
          >
            {t.tabCompleted} ({completedList.length})
          </button>
        </div>
      </div>

      {/* 추적 리스트 */}
      <div className="p-4 space-y-4 flex-1 max-w-2xl w-full mx-auto">
        {currentList.map((item) => (
          <div
            key={item.id}
            className="bg-white rounded-3xl p-5 border border-gray-100 shadow-2xs space-y-3.5 transition-all hover:border-gray-200 hover:shadow-xs"
          >
            {/* 카드 상단: 분류/색상 칩 & 우측 상태/후보 알림 */}
            <div className="flex items-center justify-between gap-2">
              {/* 좌측: 조건 칩 (분류, 색상) */}
              <div className="flex flex-wrap items-center gap-1.5">
                {item.category && (
                  <span className="bg-rose-50 text-[#85132d] border border-rose-200/60 text-xs font-semibold px-2.5 py-0.5 rounded-full">
                    {item.category}
                  </span>
                )}
                {item.color && item.color !== "미지정" && (
                  <span className="bg-gray-100 text-gray-700 text-xs font-medium px-2.5 py-0.5 rounded-full">
                    {item.color}
                  </span>
                )}
              </div>

              {/* 우측: 진행 중(새 후보 발견 or 후보 0건) / 종료됨(상태 뱃지) */}
              {activeTab === "active" ? (
                item.candidatesCount > 0 ? (
                  <button
                    type="button"
                    onClick={() => handleCandidateClick(item)}
                    className="px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-[#85132d] border border-rose-200/80 hover:bg-rose-100 hover:border-rose-300 transition-all flex items-center gap-1 active:scale-95 shadow-2xs cursor-pointer"
                  >
                    <span>
                      {lang === "en"
                        ? `${item.candidatesCount} new candidate${item.candidatesCount > 1 ? "s" : ""} found`
                        : `새 후보 ${item.candidatesCount}건 발견`}
                    </span>
                    <ExternalLink className="w-3 h-3 stroke-[2.2]" />
                  </button>
                ) : (
                  <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-gray-50 text-gray-400 border border-gray-100">
                    {t.noCandidates}
                  </span>
                )
              ) : (
                <div className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600 border border-gray-200/60">
                  <span>{getStatusText(item.statusText)}</span>
                </div>
              )}
            </div>

            {/* 카드 본문: 분실물 타이틀 및 등록했던 자연어 분실 설명 */}
            <div className="space-y-2.5">
              <h2 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
                {item.title || item.name}
              </h2>

              {/* 사용자가 등록했던 자연어 분실 설명 말풍선 */}
              {item.prompt && (
                <div className="bg-[#f8f9fa] border border-gray-100/90 rounded-2xl p-3 sm:p-3.5 flex items-start gap-2.5">
                  <MessageSquareQuote className="w-4 h-4 text-[#85132d] shrink-0 mt-0.5" />
                  <p className="text-xs sm:text-sm text-gray-700 leading-relaxed font-normal">
                    {item.prompt}
                  </p>
                </div>
              )}
            </div>

            {/* 카드 하단 메타 & 종료 버튼 */}
            <div className="pt-3 border-t border-gray-100/80 flex items-center justify-between text-xs text-gray-500 font-medium">
              <div className="space-y-0.5">
                <div>
                  <span className="text-gray-400">{t.registeredAt}:</span> {item.registeredDate}
                </div>
                <div>
                  <span className="text-gray-400">{t.expiresAt}:</span> {item.endDate}{" "}
                  {item.dDay && (
                    <span className="font-bold text-[#85132d] ml-1">({item.dDay})</span>
                  )}
                </div>
              </div>

              {/* 추적 종료 버튼 */}
              {activeTab === "active" && (
                <button
                  type="button"
                  onClick={() => setItemToConfirm(item)}
                  className="px-3 py-1.5 rounded-xl text-xs font-medium text-gray-500 hover:text-gray-800 bg-white hover:bg-gray-50 border border-gray-200/90 transition-all active:scale-95 cursor-pointer shadow-2xs"
                  title={t.endTrackingBtn}
                >
                  {t.endTrackingBtn}
                </button>
              )}
            </div>
          </div>
        ))}

        {/* 빈 목록 Empty State */}
        {currentList.length === 0 && (
          <div className="text-center py-16 px-6 bg-white rounded-3xl border border-gray-100 shadow-2xs flex flex-col items-center justify-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-[#85132d] flex items-center justify-center mb-1">
              <Search className="w-6 h-6 stroke-[2]" />
            </div>
            <p className="text-sm font-bold text-gray-800">
              {activeTab === "active"
                ? t.emptyActiveTitle
                : t.emptyCompletedTitle}
            </p>
            <p className="text-xs text-gray-400 max-w-xs">
              {t.emptyDesc}
            </p>
            <button
              type="button"
              onClick={() => router.push("/search")}
              className="mt-2 px-4 py-2.5 bg-[#85132d] text-white text-xs font-bold rounded-xl shadow-xs hover:bg-[#6e0f25] transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
            >
              <Search className="w-3.5 h-3.5" />
              <span>{t.searchLostItemBtn}</span>
            </button>
          </div>
        )}
      </div>

      {/* 정말로 종료하시겠습니까 확인 팝업 모달 */}
      {itemToConfirm && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl border border-gray-100 animate-in zoom-in-95 text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-[#85132d] flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6 stroke-[2.2]" />
            </div>

            <div className="space-y-1.5">
              <h3 className="font-bold text-gray-900 text-base sm:text-lg">
                {t.confirmEndTitle}
              </h3>
              <p className="text-xs text-gray-500 leading-relaxed whitespace-pre-line">
                <span className="font-semibold text-gray-800">
                  [{itemToConfirm.title || itemToConfirm.name}]
                </span>
                <br />
                {t.confirmEndDesc}
              </p>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => setItemToConfirm(null)}
                className="flex-1 py-3 px-4 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                {t.cancelBtn}
              </button>
              <button
                type="button"
                onClick={() => {
                  stopTracking(itemToConfirm.id);
                  setItemToConfirm(null);
                }}
                className="flex-1 py-3 px-4 rounded-xl bg-[#85132d] hover:bg-[#6e0f25] text-white font-bold text-xs sm:text-sm transition-colors cursor-pointer shadow-xs active:scale-95"
              >
                {t.confirmEndBtn}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
