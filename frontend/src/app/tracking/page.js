"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  Clock,
  ExternalLink,
  Trash2,
  MapPin,
  MessageSquareQuote,
  Settings2,
  CheckCircle2,
  Search,
  X
} from "lucide-react";
import LanguageSelector from "@/components/common/LanguageSelector";
import { useApp } from "@/context/AppContext";

export default function TrackingPage() {
  const router = useRouter();
  const { lang, setLang, t, trackingData, stopTracking, extendTracking } = useApp();

  const [activeTab, setActiveTab] = useState("active");
  const [editingItem, setEditingItem] = useState(null);

  const formatLocation = (loc) => {
    if (!loc) return "알 수 없음";
    const trimmed = loc.trim();
    if (
      trimmed === "전체" ||
      trimmed.includes("전체/") ||
      trimmed.includes("미지정") ||
      trimmed.includes("알수없음") ||
      trimmed.includes("알 수 없음")
    ) {
      return "알 수 없음";
    }
    return trimmed;
  };

  const activeList = trackingData.active || [];
  const completedList = trackingData.completed || [];
  const currentList = activeTab === "active" ? activeList : completedList;

  const handleCandidateClick = (item) => {
    // 키워드 기반으로 /search 검색 페이지 연동
    const query = item.title || item.name || item.category;
    router.push(`/search?q=${encodeURIComponent(query)}`);
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
            aria-label="뒤로가기"
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

      {/* 추적 리스트 (AI 분실물 추적 세션 카드) */}
      <div className="p-4 space-y-4 flex-1 max-w-2xl w-full mx-auto">
        {currentList.map((item) => (
          <div
            key={item.id}
            className="bg-white rounded-3xl p-5 border border-gray-100 shadow-2xs space-y-3.5 transition-all hover:border-gray-200 hover:shadow-xs"
          >
            {/* 카드 상단: 분류/색상/위치 칩 & 우측 상태/후보 알림 */}
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

              {/* 우측: 진행 중(새 후보 발견 or 이메일 알림 대기) / 종료됨(수령 완료 or 종료) */}
              {activeTab === "active" ? (
                item.candidatesCount > 0 ? (
                  <button
                    type="button"
                    onClick={() => handleCandidateClick(item)}
                    className="px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-[#85132d] border border-rose-200/80 hover:bg-rose-100 hover:border-rose-300 transition-all flex items-center gap-1 active:scale-95 shadow-2xs cursor-pointer"
                  >
                    <span>새 후보 {item.candidatesCount}건 발견</span>
                    <ExternalLink className="w-3 h-3 stroke-[2.2]" />
                  </button>
                ) : (
                  <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-gray-50 text-gray-400 border border-gray-100">
                    후보 0건
                  </span>
                )
              ) : item.statusText === "수령 완료" ? (
                <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200/80">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>수령 완료</span>
                </div>
              ) : (
                <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600 border border-gray-200/60">
                  <span>{item.statusText || "기한 만료"}</span>
                </div>
              )}
            </div>

            {/* 카드 본문: 분실물 타이틀 및 등록했던 자연어 분실 설명 */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between">
                <h2 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
                  {item.title || item.name}
                </h2>
                {item.location && (
                  <span className="text-xs text-gray-500 font-medium flex items-center gap-1 bg-gray-50 px-2 py-0.5 rounded-lg border border-gray-100 shrink-0">
                    <MapPin className="w-3 h-3 text-gray-400 shrink-0" />
                    <span>{formatLocation(item.location)}</span>
                  </span>
                )}
              </div>

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

            {/* 카드 하단 메타 & 관리 버튼 */}
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

              {activeTab === "active" && (
                <button
                  type="button"
                  onClick={() => setEditingItem(item)}
                  className="px-3 py-1.5 rounded-xl bg-gray-50 hover:bg-gray-100 border border-gray-200/80 text-gray-700 text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer"
                >
                  <Settings2 className="w-3.5 h-3.5 text-gray-500" />
                  <span>{t.editBtn}</span>
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
                ? "추적 중인 분실물이 없습니다."
                : "종료된 분실물 추적 내역이 없습니다."}
            </p>
            <p className="text-xs text-gray-400 max-w-xs">
              새로운 분실물을 검색하고 실시간 AI 모니터링 알림을 등록해보세요.
            </p>
            <button
              type="button"
              onClick={() => router.push("/search")}
              className="mt-2 px-4 py-2.5 bg-[#85132d] text-white text-xs font-bold rounded-xl shadow-xs hover:bg-[#6e0f25] transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
            >
              <Search className="w-3.5 h-3.5" />
              <span>분실물 검색하러 가기</span>
            </button>
          </div>
        )}
      </div>

      {/* 세션 관리 팝업 모달 */}
      {editingItem && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl p-5 sm:p-6 max-w-sm w-full shadow-2xl border border-gray-100 animate-in zoom-in-95 space-y-4">
            <div className="flex items-center justify-between pb-1 border-b border-gray-100">
              <div>
                <h3 className="font-bold text-gray-900 text-base">
                  추적 세션 관리
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  {editingItem.title || editingItem.name}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditingItem(null)}
                className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
                aria-label="닫기"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2 pt-1">
              <button
                type="button"
                onClick={() => {
                  extendTracking(editingItem.id);
                  setEditingItem(null);
                }}
                className="w-full py-3.5 px-4 rounded-2xl bg-[#fff1f3] hover:bg-[#ffe4e8] text-[#85132d] font-bold text-xs sm:text-sm flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4" />
                  <span>추적 기간 7일 연장</span>
                </div>
                <span className="text-[11px] font-medium opacity-80">+7일</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  stopTracking(editingItem.id);
                  setEditingItem(null);
                }}
                className="w-full py-3.5 px-4 rounded-2xl bg-gray-50 hover:bg-rose-50 text-gray-700 hover:text-rose-600 font-bold text-xs sm:text-sm flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-2">
                  <Trash2 className="w-4 h-4" />
                  <span>추적 종료 처리</span>
                </div>
                <span className="text-[11px] font-medium text-gray-400">완료 보관</span>
              </button>
            </div>

            <button
              type="button"
              onClick={() => setEditingItem(null)}
              className="w-full text-center text-xs text-gray-400 hover:text-gray-600 font-medium pt-1 cursor-pointer"
            >
              닫기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
