"use client";

import React, { useState, use } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronLeft,
  Calendar,
  MapPin,
  Building2,
  FolderArchive,
  Palette,
  FileText,
  PhoneCall,
  Copy,
  Check,
  Package
} from "lucide-react";
import { useApp } from "@/context/AppContext";

export default function ItemDetailPage({ params }) {
  const router = useRouter();
  const unwrappedParams = use(params);
  const itemId = unwrappedParams.id;

  const { lang, t, items, showToast } = useApp();

  const item = items.find((x) => x.id === itemId || x.atcId === itemId) || items[0];

  const [showContactModal, setShowContactModal] = useState(false);
  const [copiedPhone, setCopiedPhone] = useState(false);

  if (!item) return null;

  // 단일 이미지 추출
  const imageUrl =
    item.fdFilePathImg ||
    (Array.isArray(item.images) && item.images.length > 0
      ? item.images[0]
      : typeof item.images === "string"
      ? item.images
      : item.image || "");

  // 데이터 필드 정규화
  const itemName = item.fdPrdtNm || item.name || "분실물";
  const storageDate = item.fdYmd || item.date || "정보 없음";
  const foundLocation = item.fdPlace || item.location || "정보 없음";
  const category = item.prdtClNm || item.category || "기타";
  const color = item.color || "기타";
  const storageFacility = item.depPlace || item.orgNm || item.storageFacility || "정보 없음";
  const phone = item.tel || item.phone || "02-3149-2470";
  const description = item.uniq || item.description || "등록된 상세 설명이 없습니다.";

  const copyPhoneNumber = () => {
    if (typeof window !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(phone);
      setCopiedPhone(true);
      setTimeout(() => setCopiedPhone(false), 2000);
      showToast(
        lang === "ko"
          ? "보관기관 전화번호가 복사되었습니다."
          : "Facility phone number copied.",
        "success"
      );
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-[#f8f9fa] animate-in fade-in duration-200 pb-28">
      {/* 상단 네비게이션 헤더 */}
      <header className="sticky top-0 z-30 w-full bg-white/95 backdrop-blur-md border-b border-gray-100 px-4 h-14 flex items-center justify-between">
        <button
          type="button"
          onClick={() => router.back()}
          className="p-2 -ml-2 rounded-full hover:bg-gray-100 text-gray-700 transition-all active:scale-95 cursor-pointer"
          aria-label="뒤로가기"
        >
          <ChevronLeft className="w-6 h-6 stroke-[2.2]" />
        </button>

        <h1 className="text-base font-bold text-gray-900 tracking-[-0.01em]">
          분실물 상세 정보
        </h1>

        <div className="w-8" />
      </header>

      {/* 1. 단일 메인 이미지 (인셋 라운드 카드 형태) */}
      <div className="p-4 pb-2">
        <div className="relative w-full aspect-[4/3] rounded-3xl overflow-hidden bg-gray-100 select-none shadow-2xs border border-gray-200/60">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={`${itemName} 사진`}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-gray-400 gap-2 bg-gray-50">
              <Package className="w-12 h-12 stroke-[1.5]" />
              <span className="text-xs font-medium text-gray-500">등록된 이미지가 없습니다.</span>
            </div>
          )}
        </div>
      </div>

      {/* 2. 분류/색상 뱃지 및 분실물 이름 헤딩 */}
      <div className="px-5 pt-2 pb-3">
        <div className="flex items-center gap-1.5 mb-2 flex-wrap">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#85132d]/10 text-[#85132d]">
            {category}
          </span>
          {color && color !== "기타" && (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-gray-200/70 text-gray-700">
              {color}
            </span>
          )}
        </div>

        <h2 className="text-xl sm:text-2xl font-extrabold text-gray-900 tracking-[-0.02em] leading-[1.3] break-keep">
          {itemName}
        </h2>
      </div>

      {/* 3. 5대 핵심 스펙 리스트 (구분선 남발 제거 및 컬러 틴트 아이콘 적용) */}
      <div className="px-5 space-y-3.5">
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-gray-100 shadow-2xs divide-y divide-gray-100">
          {/* 보관일자 */}
          <div className="flex items-center justify-between py-3 first:pt-1 last:pb-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-rose-50 text-[#85132d] flex items-center justify-center shrink-0">
                <Calendar className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold text-gray-500">
                {t.storageDate || "보관일자"}
              </span>
            </div>
            <span className="text-xs sm:text-sm font-bold text-gray-900 tracking-[-0.01em]">
              {storageDate}
            </span>
          </div>

          {/* 습득장소 */}
          <div className="flex items-center justify-between py-3 first:pt-1 last:pb-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                <MapPin className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold text-gray-500">
                {t.foundLocation || "습득장소"}
              </span>
            </div>
            <span className="text-xs sm:text-sm font-bold text-gray-900 tracking-[-0.01em] text-right truncate max-w-[55%]">
              {foundLocation}
            </span>
          </div>

          {/* 분류 */}
          <div className="flex items-center justify-between py-3 first:pt-1 last:pb-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                <FolderArchive className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold text-gray-500">
                {t.categoryLabel || "분류"}
              </span>
            </div>
            <span className="text-xs sm:text-sm font-bold text-gray-900 tracking-[-0.01em] text-right truncate max-w-[55%]">
              {category}
            </span>
          </div>

          {/* 색상 */}
          <div className="flex items-center justify-between py-3 first:pt-1 last:pb-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                <Palette className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold text-gray-500">
                {t.colorLabel || "색상"}
              </span>
            </div>
            <span className="text-xs sm:text-sm font-bold text-gray-900 tracking-[-0.01em]">
              {color}
            </span>
          </div>

          {/* 보관 장소 */}
          <div className="flex items-center justify-between py-3 first:pt-1 last:pb-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                <Building2 className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold text-gray-500">
                {t.storageFacility || "보관 장소"}
              </span>
            </div>
            <span className="text-xs sm:text-sm font-bold text-gray-900 tracking-[-0.01em] text-right truncate max-w-[55%]">
              {storageFacility}
            </span>
          </div>
        </div>

        {/* 4. 분실물 상세 설명 (독립 클린 카드) */}
        <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-2xs space-y-2.5">
          <div className="flex items-center gap-2 text-gray-800">
            <FileText className="w-4 h-4 text-gray-400" />
            <span className="text-xs sm:text-sm font-bold tracking-tight">
              {t.detailDesc || "분실물 상세 설명"}
            </span>
          </div>
          <div className="text-xs sm:text-sm text-gray-600 leading-relaxed font-normal whitespace-pre-line tracking-[-0.01em] bg-gray-50/60 p-3.5 rounded-2xl border border-gray-100">
            {description}
          </div>
        </div>
      </div>

      {/* 최하단 고정: 보관기관 문의하기 버튼 (Full Width) */}
      <div className="sticky bottom-14 left-0 right-0 p-4 bg-gradient-to-t from-white via-white/95 to-transparent z-20">
        <button
          type="button"
          onClick={() => setShowContactModal(true)}
          className="w-full h-14 rounded-2xl bg-[#85132d] hover:bg-[#701025] text-white font-bold text-base shadow-lg shadow-[#85132d]/25 transition-all active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer"
        >
          <PhoneCall className="w-5 h-5" />
          <span>{t.contactFacility || "보관기관 문의하기"}</span>
        </button>
      </div>

      {/* 보관기관 문의 팝업 모달 */}
      {showContactModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl border border-gray-100 animate-in zoom-in-95 space-y-4">
            <div className="flex items-center gap-3.5">
              <div className="w-12 h-12 rounded-2xl bg-[#fff1f3] text-[#85132d] flex items-center justify-center shrink-0">
                <Building2 className="w-6 h-6" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[11px] font-semibold text-gray-400 mb-0.5">보관 기관명</p>
                <h3 className="font-bold text-gray-900 text-base leading-snug break-keep">
                  {storageFacility}
                </h3>
              </div>
            </div>

            <div className="bg-gray-50 p-4 rounded-2xl flex items-center justify-between border border-gray-100">
              <span className="text-xs text-gray-500 font-medium">전화번호</span>
              <span className="text-base font-bold text-[#85132d] tracking-tight">{phone}</span>
            </div>

            <div className="grid grid-cols-2 gap-2.5 pt-1">
              <button
                type="button"
                onClick={copyPhoneNumber}
                className="py-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-bold text-gray-800 transition-colors cursor-pointer flex items-center justify-center gap-1.5"
              >
                {copiedPhone ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedPhone ? "복사 완료" : "번호 복사"}</span>
              </button>
              <a
                href={`tel:${phone}`}
                onClick={() => setShowContactModal(false)}
                className="py-3 rounded-xl bg-[#85132d] hover:bg-[#701025] text-xs font-bold text-white text-center transition-colors flex items-center justify-center gap-1.5 cursor-pointer shadow-sm shadow-[#85132d]/20"
              >
                <PhoneCall className="w-3.5 h-3.5" />
                <span>전화 걸기</span>
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
