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

  const addTracking = (itemName, category = "지갑", color = "검정색", location = "서울역 일대") => {
    const newTrack = {
      id: `track-${Date.now()}`,
      name: itemName || "새 분실물",
      registeredDate: new Date().toISOString().slice(0, 10),
      endDate: "2025-09-28",
      dDay: "D-7",
      candidatesCount: 0,
      category,
      color,
      location,
      thumbnail: "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=300&q=80"
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
    showToast(lang === "ko" ? "추적이 종료되었습니다." : "Tracking has ended.", "info");
  };

  const extendTracking = (trackId) => {
    setTrackingData((prev) => ({
      ...prev,
      active: prev.active.map((it) =>
        it.id === trackId ? { ...it, dDay: "D-14" } : it
      )
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
