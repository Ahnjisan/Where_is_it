"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";

export default function AlertsPage() {
  const router = useRouter();

  const alerts = [
    {
      id: "a1",
      title: "새로운 습득물 후보 발견!",
      desc: "추적 등록하신 '지갑 (검정색)'과 일치율 98%인 습득물이 서울역에서 접수되었습니다.",
      time: "10분 전",
      isUnread: true,
      itemId: "item-1"
    },
    {
      id: "a2",
      title: "추적 기간 만료 예정 안내",
      desc: "'가방 (회색)'의 AI 추적 기간이 4일 후 종료됩니다.",
      time: "어제",
      isUnread: true
    },
    {
      id: "a3",
      title: "회원가입 완료",
      desc: "어디갔지에 오신 것을 환영합니다! AI 분실물 찾기를 시작해 보세요.",
      time: "3일 전",
      isUnread: false
    }
  ];

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-20">
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
            알림 센터
          </h1>
        </div>
        <span className="text-xs text-[#85132d] font-bold">읽지 않음 2건</span>
      </header>

      <div className="p-4 space-y-2.5">
        {alerts.map((a) => (
          <div
            key={a.id}
            onClick={() => a.itemId && router.push(`/items/${a.itemId}`)}
            className={`p-4 rounded-2xl border transition-all cursor-pointer ${
              a.isUnread
                ? "bg-white border-[#fecdd3] shadow-2xs hover:border-[#85132d]"
                : "bg-gray-50/80 border-gray-100 text-gray-500"
            }`}
          >
            <div className="flex items-start justify-between gap-2 mb-1">
              <span
                className={`text-xs sm:text-sm font-bold ${
                  a.isUnread ? "text-[#85132d]" : "text-gray-700"
                }`}
              >
                {a.title}
              </span>
              <span className="text-[11px] text-gray-400 shrink-0">{a.time}</span>
            </div>
            <p className="text-xs text-gray-600 leading-relaxed">{a.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
