"use client";

import React, { useState, useRef, useEffect } from "react";
import { ChevronDown, Globe } from "lucide-react";

export default function LanguageSelector({ currentLang = "ko", onChangeLang, variant = "badge" }) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  const languages = [
    { code: "ko", label: "한국어", sub: "KO" },
    { code: "en", label: "English", sub: "EN" }
  ];

  const currentOption = languages.find((l) => l.code === currentLang) || languages[0];

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (variant === "form") {
    // 회원가입 폼 내부의 풀 너비 셀렉트 스타일
    return (
      <div className="relative" ref={dropdownRef}>
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="w-full h-13 px-4 rounded-2xl bg-gray-50 hover:bg-gray-100/80 border border-gray-200/80 text-left text-sm font-medium text-gray-800 flex items-center justify-between transition-all focus:outline-none focus:ring-2 focus:ring-[#85132d]/20 focus:border-[#85132d]"
        >
          <div className="flex items-center gap-2.5 text-gray-500">
            <Globe className="w-5 h-5 text-gray-400" />
            <span className="text-gray-900 font-medium">{currentOption.label}</span>
          </div>
          <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? "rotate-180" : ""}`} />
        </button>

        {isOpen && (
          <div className="absolute top-full left-0 right-0 mt-2 py-1.5 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 animate-in fade-in zoom-in-95">
            {languages.map((lang) => (
              <button
                key={lang.code}
                type="button"
                onClick={() => {
                  onChangeLang(lang.code);
                  setIsOpen(false);
                }}
                className={`w-full px-4 py-2.5 text-left text-sm font-medium flex items-center justify-between transition-colors ${
                  currentLang === lang.code ? "bg-[#fff1f3] text-[#85132d]" : "text-gray-700 hover:bg-gray-50"
                }`}
              >
                <span>{lang.label}</span>
                <span className="text-xs text-gray-400 uppercase">{lang.sub}</span>
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  // 상단 네비게이션 헤더용 미니 캡슐 버튼
  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gray-100/80 hover:bg-gray-200/70 text-gray-700 text-xs font-semibold tracking-tight transition-all active:scale-95"
      >
        <span>{currentOption.label}</span>
        <ChevronDown className={`w-3.5 h-3.5 text-gray-500 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-32 py-1 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 animate-in fade-in zoom-in-95">
          {languages.map((lang) => (
            <button
              key={lang.code}
              type="button"
              onClick={() => {
                onChangeLang(lang.code);
                setIsOpen(false);
              }}
              className={`w-full px-3.5 py-2 text-left text-xs font-semibold flex items-center justify-between transition-colors ${
                currentLang === lang.code ? "bg-[#fff1f3] text-[#85132d]" : "text-gray-700 hover:bg-gray-50"
              }`}
            >
              <span>{lang.label}</span>
              <span className="text-[10px] text-gray-400 uppercase">{lang.sub}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
