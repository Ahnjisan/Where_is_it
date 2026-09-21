"use client";

import React, { useState } from "react";
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
  ExternalLink,
  Check
} from "lucide-react";
import { I18N } from "@/lib/mockData";

export default function ItemDetailView({
  item,
  onBack,
  lang = "ko",
  showToast,
  onToggleLike
}) {
  const t = I18N[lang] || I18N.ko;

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

  const handleContactFacility = () => {
    setShowContactModal(true);
  };

  const copyPhoneNumber = () => {
    navigator.clipboard?.writeText(item.phone || "02-3149-2470");
    showToast(
      lang === "ko"
        ? "보관기관 전화번호가 복사되었습니다."
        : "Facility phone number copied to clipboard.",
      "success"
    );
  };

  const detailRows = [
    {
      icon: Calendar,
      label: t.foundDate,
      value: item.date
    },
    {
      icon: MapPin,
      label: t.foundLocation,
      value: item.location
    },
    {
      icon: Building2,
      label: t.storageFacility,
      value: item.storageFacility
    },
    {
      icon: FolderArchive,
      label: t.categoryLabel,
      value: item.category
    },
    {
      icon: Palette,
      label: t.colorLabel,
      value: item.color
    },
    {
      icon: FileText,
      label: t.detailDesc,
      value: item.description,
      isLong: true
    }
  ];

  return (
    <div className="flex-1 flex flex-col bg-white max-w-lg mx-auto w-full animate-in fade-in duration-300 pb-20">
      {/* 갤러리 이미지 슬라이더 (와이어프레임 05) */}
      <div className="relative w-full aspect-square sm:aspect-4/3 bg-gray-100 overflow-hidden select-none">
        <img
          src={images[currentImageIndex] || "https://images.unsplash.com/photo-1627123424574-724758594e93?w=800"}
          alt={`${item.name} 사진`}
          className="w-full h-full object-cover transition-all duration-300"
        />

        {/* 좌우 이동 화살표 */}
        {images.length > 1 && (
          <>
            <button
              type="button"
              onClick={handlePrevImage}
              className="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-all active:scale-90"
              aria-label="이전 사진"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={handleNextImage}
              className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-all active:scale-90"
              aria-label="다음 사진"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </>
        )}

        {/* 인디케이터 뱃지 (1/3) */}
        {images.length > 0 && (
          <div className="absolute bottom-3.5 right-3.5 px-2.5 py-1 rounded-full bg-black/60 text-white text-xs font-semibold backdrop-blur-xs">
            {currentImageIndex + 1} / {images.length}
          </div>
        )}
      </div>

      {/* 분실물 기본 정보 & 일치율 */}
      <div className="p-5 border-b border-gray-100">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl sm:text-2xl font-extrabold text-gray-900 tracking-tight">
            {item.name}
          </h2>
          {/* 일치율 뱃지 */}
          <span className="shrink-0 px-3 py-1 rounded-full text-xs sm:text-sm font-bold bg-emerald-50 text-emerald-600 border border-emerald-200/70 shadow-2xs">
            {item.matchRate}% {t.matchSuffix}
          </span>
        </div>
      </div>

      {/* 상세 정보 리스트 (토스 스타일 깔끔한 카드 리스트) */}
      <div className="p-5 space-y-4">
        <div className="bg-gray-50/70 rounded-2xl p-4 border border-gray-100 space-y-3.5">
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
                <div className={`font-medium text-gray-900 ${row.isLong ? "pl-6 text-gray-700 leading-relaxed text-xs sm:text-sm bg-white p-3 rounded-xl border border-gray-100 w-full mt-1" : "text-right truncate"}`}>
                  {row.value}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 최하단 고정 버튼: 보관기관 문의하기 (와이어프레임 05) */}
      <div className="sticky bottom-14 left-0 right-0 p-4 bg-gradient-to-t from-white via-white/95 to-transparent z-20">
        <div className="max-w-md mx-auto">
          <button
            type="button"
            onClick={handleContactFacility}
            className="w-full h-14 rounded-2xl bg-[#85132d] hover:bg-[#701025] text-white font-bold text-base shadow-lg shadow-[#85132d]/25 transition-all active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
          >
            <PhoneCall className="w-5 h-5" />
            <span>{t.contactFacility}</span>
          </button>
        </div>
      </div>

      {/* 보관기관 문의하기 팝업 모달 */}
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
                className="py-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-bold text-gray-800 transition-colors"
              >
                번호 복사
              </button>
              <a
                href={`tel:${item.phone || "02-3149-2470"}`}
                onClick={() => setShowContactModal(false)}
                className="py-3 rounded-xl bg-[#85132d] hover:bg-[#701025] text-xs font-bold text-white text-center transition-colors flex items-center justify-center gap-1"
              >
                전화 걸기
              </a>
            </div>

            <button
              type="button"
              onClick={() => setShowContactModal(false)}
              className="w-full text-center text-xs text-gray-400 hover:text-gray-600 font-medium pt-1"
            >
              닫기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
