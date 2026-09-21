"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Search, Compass, Bell, User } from "lucide-react";
import { useApp } from "@/context/AppContext";

export default function BottomNav() {
  const pathname = usePathname();
  const { t } = useApp();

  const navItems = [
    { href: "/", label: t.navHome, icon: Home },
    { href: "/search", label: t.navSearch, icon: Search },
    { href: "/tracking", label: t.navTrack, icon: Compass },
    { href: "/alerts", label: t.navAlert, icon: Bell, badge: 2 },
    { href: "/my", label: t.navMy, icon: User }
  ];

  return (
    <nav className="w-full bg-white/95 backdrop-blur-md border-t border-gray-100 px-3 py-2 z-40 shadow-[0_-4px_20px_rgba(0,0,0,0.03)] shrink-0 sticky bottom-0">
      <div className="max-w-md mx-auto flex items-center justify-around">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive =
            item.href === "/"
              ? pathname === "/"
              : pathname.startsWith(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className="relative flex flex-col items-center justify-center py-1 px-3 rounded-2xl transition-all duration-200 active:scale-95 group"
            >
              <div className="relative">
                <Icon
                  className={`w-5 h-5 transition-transform duration-200 ${
                    isActive
                      ? "text-[#85132d] scale-110 stroke-[2.4]"
                      : "text-gray-400 group-hover:text-gray-600 stroke-[1.8]"
                  }`}
                />
                {item.badge > 0 && (
                  <span className="absolute -top-1 -right-2 w-4 h-4 bg-[#85132d] text-white text-[10px] font-bold rounded-full flex items-center justify-center border-2 border-white animate-pulse">
                    {item.badge}
                  </span>
                )}
              </div>
              <span
                className={`text-[11px] mt-1 font-semibold transition-colors duration-200 ${
                  isActive ? "text-[#85132d]" : "text-gray-400 group-hover:text-gray-600"
                }`}
              >
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
