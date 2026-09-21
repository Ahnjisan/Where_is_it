"use client";

import React, { useEffect } from "react";
import { CheckCircle2, AlertCircle, Info, X } from "lucide-react";

export default function Toast({ message, type = "success", onClose, duration = 3000 }) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => {
      onClose();
    }, duration);
    return () => clearTimeout(timer);
  }, [message, duration, onClose]);

  if (!message) return null;

  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
    error: <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />,
    info: <Info className="w-5 h-5 text-sky-400 shrink-0" />
  };

  return (
    <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[100] w-[90%] max-w-sm transition-all duration-300 animate-in fade-in slide-in-from-top-4">
      <div className="flex items-center gap-3 px-4 py-3.5 rounded-2xl bg-[#191f28]/95 text-white shadow-xl backdrop-blur-md border border-white/10 text-sm font-medium">
        {icons[type] || icons.info}
        <span className="flex-1 leading-snug">{message}</span>
        <button
          onClick={onClose}
          className="p-1 rounded-full hover:bg-white/10 text-gray-300 transition-colors"
          aria-label="닫기"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
