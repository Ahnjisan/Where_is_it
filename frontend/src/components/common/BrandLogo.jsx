"use client";

import React from "react";
import BrandIcon from "./BrandIcon";

export default function BrandLogo({
  size = "md",
  subtitle = "",
  align = "center",
  lang = "ko",
  clickable = true,
}) {
  const isKo = lang === "ko";
  const titleText = isKo ? "어디갔지" : "Where Is It";

  if (size === "sm") {
    return (
      <div className={`flex items-center gap-2.5 select-none ${clickable ? "cursor-pointer group" : "cursor-default"}`}>
        <BrandIcon
          className={`w-8 h-8 rounded-xl shadow-xs transition-transform duration-200 ${clickable ? "group-hover:scale-105" : ""}`}
          size={32}
        />
        <span className="text-xl font-extrabold tracking-tight text-[#191f28] leading-none">
          {titleText}
        </span>
      </div>
    );
  }

  return (
    <div
      className={`flex flex-col ${
        align === "center" ? "items-center text-center" : "items-start text-left"
      } select-none`}
    >
      <div className="mb-3.5 transition-transform duration-200 hover:scale-105">
        <BrandIcon className="w-16 h-16 rounded-2xl shadow-md" size={64} />
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
