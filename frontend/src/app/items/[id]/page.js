"use client";

import React, { useState, use } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  ChevronRight,
  Calendar,
  MapPin,
  Building2,
  FolderArchive,
  Palette,
  FileText,
  PhoneCall,
  Clock,
  Heart,
  Share2
} from "lucide-react";
import { useApp } from "@/context/AppContext";

export default function ItemDetailPage({ params }) {
  const router = useRouter();
  const unwrappedParams = use(params);
  const itemId = unwrappedParams.id;

  const { lang, t, items, toggleLike, showToast } = useApp();

  const item = items.find((x) => x.id === itemId) || items[0];

  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [showContactModal, setShowContactModal] = useState(false);

  if (!item) return null;

  const images = item.images && item.images.length > 0 ? item.images : [];

  const handlePrevImage = () => {
    setCurrentImageIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const handleNextImage = () => {
    setCurrentImageIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  const handleShare = () => {
    if (typeof window !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      showToast(
        lang === "ko" ? "링크가 클립보드에 복사되었습니다." : "Link copied to clipboard.",
        "success"
      );
    }
  };

  const copyPhoneNumber = () => {
    if (typeof window !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(item.phone || "02-3149-2470");
      showToast(
        lang === "ko"
          ? "보관기관 전화번호가 복사되었습니다."
          : "Facility phone number copied.",
        "success"
      );
    }
  };

  const detailRows = [
    { icon: Calendar, label: t.foundDate, value: item.date },
    { icon: MapPin, label: t.foundLocation, value: item.location },
    { icon: Building2, label: t.storageFacility, value: item.storageFacility },
    { icon: FolderArchive, label: t.categoryLabel, value: item.category },
    { icon: Palette, label: t.colorLabel, value: item.color },
    { icon: FileText, label: t.detailDesc, value: item.description, isLong: true }
  ];

  return (
    <div className="flex-1 flex flex-col bg-white animate-in fade-in duration-200 pb-20">
      {/* 상단 네비게이션 헤더 (와이어프레임 05) */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <button
          type="button"
          onClick={() => router.back()}
          className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
          aria-label="뒤로가기"
        >
          <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
        </button>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => toggleLike(item.id)}
            className="p-2 rounded-full hover:bg-gray-100 transition-all active:scale-90 cursor-pointer"
            aria-label="관심 저장"
          >
            <Heart
              className={`w-5 h-5 transition-colors ${
                item.isLiked
                  ? "fill-rose-500 text-rose-500"
                  : "text-gray-600 hover:text-gray-900"
              }`}
            />
          </button>
          <button
            type="button"
            onClick={handleShare}
            className="p-2 rounded-full hover:bg-gray-100 text-gray-600 hover:text-gray-900 transition-all active:scale-90 cursor-pointer"
            aria-label="공유하기"
          >
            <Share2 className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* 갤러리 이미지 슬라이더 */}
      <div className="relative w-full aspect-square bg-gray-100 overflow-hidden select-none">
        <img
          src={images[currentImageIndex]}
          alt={`${item.name} 사진`}
          className="w-full h-full object-cover transition-all duration-300"
        />

        {images.length > 1 && (
          <>
            <button
              type="button"
              onClick={handlePrevImage}
              className="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-all active:scale-90 cursor-pointer"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={handleNextImage}
              className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-all active:scale-90 cursor-pointer"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </>
        )}

        {images.length > 0 && (
          <div className="absolute bottom-3.5 right-3.5 px-2.5 py-1 rounded-full bg-black/60 text-white text-xs font-semibold backdrop-blur-xs">
            {currentImageIndex + 1} / {images.length}
          </div>
        )}
      </div>

      {/* 제목 & 일치율 */}
      <div className="p-5 border-b border-gray-100">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl sm:text-2xl font-extrabold text-gray-900 tracking-tight">
            {item.name}
          </h2>
          <span className="shrink-0 px-3 py-1 rounded-full text-xs sm:text-sm font-bold bg-emerald-50 text-emerald-600 border border-emerald-200/70 shadow-2xs">
            {item.matchRate}% {t.matchSuffix}
          </span>
        </div>
      </div>

      {/* 속성 리스트 */}
      <div className="p-5 space-y-4">
        <div className="bg-gray-50/80 rounded-2xl p-4 border border-gray-100 space-y-3.5">
          {detailRows.map((row, index) => {
            const Icon = row.icon;
            return (
              <div
                key={index}
                className={`flex ${row.isLong ? "flex-col items-start gap-1.5" : "items-center justify-between gap-3"} text-xs sm:text-sm`}
              >
                <div className="flex items-center gap-2.5 text-gray-500 shrink-0 font-semibold">
                  <Icon className="w-4 h-4 text-gray-400" />
                  <span>{row.label}</span>
                </div>
                <div className={`font-medium text-gray-900 ${row.isLong ? "text-gray-700 leading-relaxed bg-white p-3 rounded-xl border border-gray-100 w-full mt-1" : "text-right truncate"}`}>
                  {row.value}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 최하단 고정: 보관기관 문의하기 버튼 (와이어프레임 05) */}
      <div className="sticky bottom-14 left-0 right-0 p-4 bg-gradient-to-t from-white via-white/95 to-transparent z-20">
        <button
          type="button"
          onClick={() => setShowContactModal(true)}
          className="w-full h-14 rounded-2xl bg-[#85132d] hover:bg-[#701025] text-white font-bold text-base shadow-lg shadow-[#85132d]/25 transition-all active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
        >
          <PhoneCall className="w-5 h-5" />
          <span>{t.contactFacility}</span>
        </button>
      </div>

      {/* 보관기관 문의 모달 */}
      {showContactModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl border border-gray-100 animate-in zoom-in-95 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-[#fff1f3] text-[#85132d] flex items-center justify-center">
                <Building2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-base">
                  {item.storageFacility}
                </h3>
                <p className="text-xs text-gray-500 font-medium">
                  {item.location}
                </p>
              </div>
            </div>

            <div className="bg-gray-50 p-3.5 rounded-2xl space-y-2 text-xs text-gray-600">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-gray-400 shrink-0" />
                <span>운영시간: {item.operatingHours || "평일 09:00 ~ 18:00"}</span>
              </div>
              <div className="flex items-center gap-2 font-bold text-gray-800">
                <PhoneCall className="w-4 h-4 text-[#85132d] shrink-0" />
                <span>연락처: {item.phone || "02-3149-2470"}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2.5 pt-1">
              <button
                type="button"
                onClick={copyPhoneNumber}
                className="py-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-bold text-gray-800 transition-colors cursor-pointer"
              >
                번호 복사
              </button>
              <a
                href={`tel:${item.phone || "02-3149-2470"}`}
                onClick={() => setShowContactModal(false)}
                className="py-3 rounded-xl bg-[#85132d] hover:bg-[#701025] text-xs font-bold text-white text-center transition-colors flex items-center justify-center gap-1 cursor-pointer"
              >
                전화 걸기
              </a>
            </div>

            <button
              type="button"
              onClick={() => setShowContactModal(false)}
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
