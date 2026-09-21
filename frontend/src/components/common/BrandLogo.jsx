"use client";

import React from "react";
import { MapPin } from "lucide-react";

export default function BrandLogo({ size = "md", subtitle = "", align = "center", lang = "ko" }) {
  const isKo = lang === "ko";
  const titleText = isKo ? "어디갔지" : "Where Is It";

  if (size === "sm") {
    return (
      <div className="flex items-center gap-2 cursor-pointer select-none">
        <div className="w-7 h-7 rounded-full bg-[#85132d] flex items-center justify-center text-white shadow-sm">
          <MapPin className="w-4 h-4 fill-white" />
        </div>
        <span className="text-xl font-bold tracking-tight text-[#191f28]">
          {titleText}
        </span>
      </div>
    );
  }

  return (
    <div className={`flex flex-col ${align === "center" ? "items-center text-center" : "items-start text-left"} select-none`}>
      <div className="w-14 h-14 rounded-2xl bg-[#fff1f3] border border-[#fecdd3] flex items-center justify-center text-[#85132d] mb-4 shadow-sm">
        <MapPin className="w-8 h-8 fill-[#85132d]" />
      </div>
      <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#191f28] mb-2">
        {titleText}
      </h1>
      {subtitle && (
        <p className="text-sm sm:text-base text-gray-500 font-medium max-w-xs leading-relaxed">
          {subtitle}
        </p>
      )}
    </div>
  );
}
