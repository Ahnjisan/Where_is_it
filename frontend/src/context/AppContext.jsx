"use client";

import React, { createContext, useContext, useState } from "react";
import Toast from "@/components/ui/Toast";
import { INITIAL_LOST_ITEMS, INITIAL_TRACKING_LIST, I18N } from "@/lib/mockData";

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [lang, setLang] = useState("ko");
  const [items, setItems] = useState(INITIAL_LOST_ITEMS);
  const [trackingData, setTrackingData] = useState(INITIAL_TRACKING_LIST);
  const [toast, setToast] = useState({ message: "", type: "success" });
  const [user, setUser] = useState(() => {
    if (typeof window !== "undefined") {
      try {
        const saved = localStorage.getItem("where_user");
        if (saved) return JSON.parse(saved);
      } catch (e) {
        // ignore
      }
    }
    return {
      email: "user@whereisit.kr",
      joinedDate: "2025.09.01",
    };
  });

  const updateUser = (userData) => {
    setUser((prev) => {
      const updated = { ...prev, ...userData };
      if (typeof window !== "undefined") {
        try {
          localStorage.setItem("where_user", JSON.stringify(updated));
        } catch (e) {
          // ignore
        }
      }
      return updated;
    });
  };

  const logoutUser = () => {
    console.log("[AppContext] 로그아웃 실행 - 이전 사용자 정보:", user);
    if (typeof window !== "undefined") {
      try {
        localStorage.removeItem("where_user");
      } catch (e) {
        // ignore
      }
    }
    setUser(null);
    console.log("[AppContext] 사용자 세션 초기화 완료");
  };

  const t = I18N[lang] || I18N.ko;

  const showToast = (message, type = "success") => {
    setToast({ message, type });
  };

  const hideToast = () => {
    setToast({ message: "", type: "success" });
  };

  const toggleLike = (itemId) => {
    setItems((prev) =>
      prev.map((it) => {
        if (it.id === itemId) {
          const nextLike = !it.isLiked;
          showToast(nextLike ? t.toastLiked : t.toastUnliked, "info");
          return { ...it, isLiked: nextLike };
        }
        return it;
      })
    );
  };

  const addTracking = (params) => {
    let title = "분실물 추적";
    let prompt = "";
    let category = "지갑";
    let color = "검정색";
    let location = "알 수 없음";

    if (typeof params === "string") {
      title = params.trim() || "분실물 추적";
      prompt = params.trim() || "등록된 분실 정황 설명이 없습니다.";
    } else if (params && typeof params === "object") {
      title = params.title || params.itemName || params.query || "분실물 추적";
      prompt = params.prompt || params.query || params.description || title;
      category = params.category || "지갑";
      color = params.color || "검정색";
      const rawLoc = params.location ? params.location.trim() : "";
      location = (!rawLoc || rawLoc === "전체" || rawLoc.includes("전체/") || rawLoc.includes("미지정") || rawLoc.includes("알수없음") || rawLoc.includes("알 수 없음"))
        ? "알 수 없음"
        : rawLoc;
    }

    const today = new Date().toISOString().slice(0, 10);
    const end = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

    const newTrack = {
      id: `track-${Date.now()}`,
      title,
      name: title,
      prompt,
      registeredDate: today,
      endDate: end,
      dDay: "D-7",
      candidatesCount: 0,
      category,
      color,
      location
    };

    setTrackingData((prev) => ({
      ...prev,
      active: [newTrack, ...prev.active]
    }));

    showToast(t.toastTrackingSuccess, "success");
    return newTrack;
  };

  const stopTracking = (trackId) => {
    setTrackingData((prev) => {
      const itemToStop = prev.active.find((x) => x.id === trackId);
      if (!itemToStop) return prev;
      return {
        active: prev.active.filter((x) => x.id !== trackId),
        completed: [
          {
            ...itemToStop,
            statusText: "추적 종료",
            endDate: new Date().toISOString().slice(0, 10)
          },
          ...prev.completed
        ]
      };
    });
    showToast(lang === "ko" ? "추적이 종료되었습니다. (종료됨으로 이동)" : "Tracking has ended.", "info");
  };

  const extendTracking = (trackId) => {
    setTrackingData((prev) => ({
      ...prev,
      active: prev.active.map((it) => {
        if (it.id !== trackId) return it;
        const baseDate = it.endDate ? new Date(it.endDate) : new Date();
        baseDate.setDate(baseDate.getDate() + 7);
        const newEndDate = baseDate.toISOString().slice(0, 10);
        return {
          ...it,
          endDate: newEndDate,
          dDay: "D-14"
        };
      })
    }));
    showToast(
      lang === "ko" ? "추적 기간이 7일 연장되었습니다." : "Tracking period extended by 7 days.",
      "success"
    );
  };

  return (
    <AppContext.Provider
      value={{
        lang,
        setLang,
        t,
        items,
        trackingData,
        user,
        updateUser,
        logoutUser,
        showToast,
        toggleLike,
        addTracking,
        stopTracking,
        extendTracking
      }}
    >
      {children}
      <Toast
        message={toast.message}
        type={toast.type}
        onClose={hideToast}
      />
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error("useApp must be used within an AppProvider");
  }
  return context;
}
