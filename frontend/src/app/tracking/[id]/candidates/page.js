"use client";

import React, { use } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, MapPin, Calendar } from "lucide-react";
import { useApp } from "@/context/AppContext";

// Mock Data for AirPods candidates
export const mockCandidates = [
  {
    id: 101,
    title: "에어팟 프로 2세대 본체",
    category: "전자기기 > 무선이어폰",
    color: "흰색",
    location: "서울남대문경찰서",
    date: "2026-09-28",
    status: "보관중",
    matchRate: 98,
    imageUrl: "/images/purple_airpods.jpg",
    phone: "02-123-4567",
    description: "서울남대문경찰서에 보관중인 에어팟 프로 2세대 본체입니다. 케이스에 작은 기스가 있습니다."
  },
  {
    id: 102,
    title: "애플 에어팟 (세대 모름)",
    category: "전자기기 > 무선이어폰",
    color: "흰색",
    location: "강남대로 100 유실물센터",
    date: "2026-09-29",
    status: "보관중",
    matchRate: 85,
    imageUrl: "https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?auto=format&fit=crop&q=80&w=400&h=400",
    phone: "02-987-6543",
    description: "강남대로 길가에서 발견된 에어팟입니다. 세대는 정확하지 않습니다."
  },
  {
    id: 103,
    title: "에어팟 왼쪽 유닛",
    category: "전자기기 > 무선이어폰",
    color: "흰색",
    location: "스타벅스 강남대로점",
    date: "2026-09-27",
    status: "보관중",
    matchRate: 72,
    imageUrl: "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?auto=format&fit=crop&q=80&w=400&h=400",
    phone: "02-111-2222",
    description: "스타벅스 매장 내에서 습득한 에어팟 왼쪽 유닛 단품입니다."
  }
];

export default function CandidatesPage({ params }) {
  const router = useRouter();
  const unwrappedParams = use(params);
  const { lang, t } = useApp();

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-24 min-h-screen">
      {/* Header */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
          >
            <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
          </button>
          <h1 className="text-base sm:text-lg font-bold text-gray-900 tracking-tight">
            {lang === "ko" ? "발견된 후보 목록" : "Found Candidates"}
          </h1>
        </div>
        <div className="w-8" />
      </header>

      {/* Content */}
      <div className="p-4 space-y-4 flex-1 max-w-2xl w-full mx-auto">
        <div className="mb-2 bg-white p-5 rounded-3xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-gray-900">
              {lang === "ko" ? "에어팟 후보 3건 발견!" : "3 AirPods candidates found!"}
            </h2>
            <p className="text-sm text-gray-500 mt-1">
              {lang === "ko" ? "선택하시면 상세 정보를 볼 수 있습니다." : "Click to view details."}
            </p>
          </div>
          <div className="w-12 h-12 bg-rose-50 text-[#85132d] rounded-2xl flex items-center justify-center">
            <span className="font-bold text-xl">3</span>
          </div>
        </div>

        {mockCandidates.map((item) => (
          <div
            key={item.id}
            onClick={() => router.push(`/tracking/${unwrappedParams.id}/candidates/${item.id}`)}
            className="group relative bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs hover:shadow-md hover:border-gray-200 transition-all duration-200 cursor-pointer active:scale-[0.99] flex gap-3.5 items-start"
          >
            {/* 분실물 썸네일 이미지 */}
            <div className="w-20 h-20 sm:w-22 sm:h-22 rounded-xl overflow-hidden bg-gray-100 shrink-0 relative mt-0.5">
              <img
                src={item.imageUrl}
                alt={item.title}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              />
            </div>

            {/* 본문 정보 영역 */}
            <div className="flex-1 min-w-0">
              {/* 상단 라인: 분류 태그, 색상 chip */}
              <div className="flex items-center justify-between gap-2 mb-1.5">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-gray-100 text-gray-700">
                    전자기기 &gt; 무선이어폰
                  </span>
                  {item.color && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-gray-100 text-gray-700">
                      {item.color}
                    </span>
                  )}
                </div>
              </div>

              {/* 1. 분실물 이름 */}
              <h3 className="text-sm sm:text-base font-bold text-gray-900 leading-snug line-clamp-2 break-keep mb-2">
                {item.title}
              </h3>

              {/* 보관 장소 및 습득일(보관일) */}
              <div className="space-y-1 text-xs text-gray-500 font-medium pt-0.5 border-t border-gray-50">
                <div className="flex items-center gap-1.5 truncate">
                  <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                  <span>{item.date || "날짜 정보 없음"}</span>
                </div>
                <div className="flex items-center gap-1.5 truncate">
                  <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                  <span className="truncate">
                    {item.location || "장소 정보 없음"}
                  </span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
