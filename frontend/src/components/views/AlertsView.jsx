"use client";

import React from "react";
import { Bell, CheckCheck, Clock, MapPin, Sparkles } from "lucide-react";

export default function AlertsView({ onViewItem, lang = "ko" }) {
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
    <div className="flex-1 flex flex-col bg-[#f8f9fa] max-w-lg mx-auto w-full p-4 space-y-3 pb-20 animate-in fade-in">
      <div className="flex items-center justify-between pb-1">
        <h2 className="text-lg font-bold text-gray-900">알림 센터</h2>
        <span className="text-xs text-[#85132d] font-bold">읽지 않음 2건</span>
      </div>

      <div className="space-y-2.5">
        {alerts.map((a) => (
          <div
            key={a.id}
            onClick={() => a.itemId && onViewItem(a.itemId)}
            className={`p-4 rounded-2xl border transition-all cursor-pointer ${
              a.isUnread
                ? "bg-white border-[#fecdd3] shadow-xs hover:border-[#85132d]"
                : "bg-gray-50/80 border-gray-100 text-gray-500"
            }`}
          >
            <div className="flex items-start justify-between gap-2 mb-1">
              <span className={`text-xs sm:text-sm font-bold ${a.isUnread ? "text-[#85132d]" : "text-gray-700"}`}>
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
