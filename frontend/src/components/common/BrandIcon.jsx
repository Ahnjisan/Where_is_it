"use client";

import React from "react";

export default function BrandIcon({ className = "w-7 h-7", size = 28 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 select-none ${className}`}
      aria-hidden="true"
    >
      {/* 딥 버건디 스퀘어클 배경 */}
      <rect width="48" height="48" rx="13" fill="#85132d" />

      {/* 미니멀 위치 GPS 핀 (단일 패스) */}
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M24 8C17.3726 8 12 13.3726 12 20C12 27.6 21.6 37.8 23.1 39.3C23.6 39.8 24.4 39.8 24.9 39.3C26.4 37.8 36 27.6 36 20C36 13.3726 30.6274 8 24 8ZM24 15C26.7614 15 29 17.2386 29 20C29 22.7614 26.7614 25 24 25C21.2386 25 19 22.7614 19 20C19 17.2386 21.2386 15 24 15Z"
        fill="white"
      />
    </svg>
  );
}
