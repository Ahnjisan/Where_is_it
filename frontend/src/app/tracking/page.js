"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  ChevronDown,
  Clock,
  ExternalLink,
  Trash2
} from "lucide-react";
import LanguageSelector from "@/components/common/LanguageSelector";
import { useApp } from "@/context/AppContext";

export default function TrackingPage() {
  const router = useRouter();
  const { lang, setLang, t, trackingData, stopTracking, extendTracking } = useApp();

  const [activeTab, setActiveTab] = useState("active");
  const [editingItem, setEditingItem] = useState(null);

  const activeList = trackingData.active || [];
  const completedList = trackingData.completed || [];
  const currentList = activeTab === "active" ? activeList : completedList;

  const handleCandidateClick = (item) => {
    router.push(`/search?q=${encodeURIComponent(item.name)}`);
  };

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-20">
      {/* 상단 네비게이션 헤더 (와이어프레임 08) */}
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

      {/* 상단 탭 (와이어프레임 08: [진행 중 (N)] / [종료됨 (N)]) */}
      <div className="p-4 bg-white border-b border-gray-100">
        <div className="flex p-1 bg-gray-100 rounded-2xl">
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
      <div className="p-4 space-y-3.5 flex-1">
        {currentList.map((item) => (
          <div
            key={item.id}
            className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs space-y-3 transition-all hover:border-gray-200"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                {item.thumbnail && (
                  <img
                    src={item.thumbnail}
                    alt={item.name}
                    className="w-12 h-12 rounded-xl object-cover bg-gray-100 shrink-0"
                  />
                )}
                <div>
                  <h3 className="font-bold text-gray-900 text-sm sm:text-base">
                    {item.name}
                  </h3>
                  <div className="flex items-center gap-2 text-xs text-gray-400 mt-0.5 font-medium">
                    <span>{item.category}</span>
                    <span>•</span>
                    <span>{item.color}</span>
                  </div>
                </div>
              </div>

              {/* 추가 후보 뱃지 */}
              {activeTab === "active" ? (
                item.candidatesCount > 0 ? (
                  <button
                    type="button"
                    onClick={() => handleCandidateClick(item)}
                    className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-600 border border-rose-200/70 hover:bg-rose-100 transition-all flex items-center gap-1 active:scale-95 animate-pulse cursor-pointer"
                  >
                    <span>
                      {t.additionalCandidates} {item.candidatesCount}건
                    </span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                ) : (
                  <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-500">
                    후보 0건
                  </span>
                )
              ) : (
                <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-600">
                  {item.statusText || "종료"}
                </span>
              )}
            </div>

            {/* 일정 & 수정 버튼 */}
            <div className="pt-2 border-t border-gray-50 flex items-center justify-between text-xs text-gray-500 font-medium">
              <div className="space-y-1">
                <div>
                  {t.registeredAt}: {item.registeredDate}
                </div>
                <div>
                  {t.expiresAt}: {item.endDate}{" "}
                  {item.dDay && (
                    <span className="font-bold text-[#85132d]">({item.dDay})</span>
                  )}
                </div>
              </div>

              {activeTab === "active" && (
                <button
                  type="button"
                  onClick={() => setEditingItem(item)}
                  className="px-3 py-1.5 rounded-xl bg-gray-50 hover:bg-gray-100 border border-gray-200/80 text-gray-700 text-xs font-bold flex items-center gap-1 transition-all active:scale-95 cursor-pointer"
                >
                  <span>{t.editBtn}</span>
                  <ChevronDown className="w-3 h-3 text-gray-400" />
                </button>
              )}
            </div>
          </div>
        ))}

        {currentList.length === 0 && (
          <div className="text-center py-16 bg-white rounded-2xl border border-gray-100 p-6">
            <p className="text-sm font-bold text-gray-700 mb-1">
              추적 중인 분실물이 없습니다.
            </p>
            <p className="text-xs text-gray-400">
              새로운 분실물을 검색하고 추적 알림을 등록해보세요.
            </p>
          </div>
        )}
      </div>

      {/* 수정 관리 팝업 모달 */}
      {editingItem && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl border border-gray-100 animate-in zoom-in-95 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">
                추적 정보 수정
              </h3>
              <span className="text-xs text-[#85132d] font-bold">
                {editingItem.name}
              </span>
            </div>

            <div className="space-y-2 pt-1">
              <button
                type="button"
                onClick={() => {
                  extendTracking(editingItem.id);
                  setEditingItem(null);
                }}
                className="w-full py-3 px-4 rounded-xl bg-[#fff1f3] hover:bg-[#ffe4e8] text-[#85132d] font-bold text-xs flex items-center justify-between transition-colors cursor-pointer"
              >
                <span>추적 기간 7일 연장</span>
                <Clock className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={() => {
                  stopTracking(editingItem.id);
                  setEditingItem(null);
                }}
                className="w-full py-3 px-4 rounded-xl bg-gray-50 hover:bg-rose-50 text-gray-700 hover:text-rose-600 font-bold text-xs flex items-center justify-between transition-colors cursor-pointer"
              >
                <span>추적 종료 처리</span>
                <Trash2 className="w-4 h-4" />
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
