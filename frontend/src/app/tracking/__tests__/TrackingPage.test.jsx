import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import TrackingPage from "../page";

const { getMyTrackedItems, showToast } = vi.hoisted(() => ({
  getMyTrackedItems: vi.fn(),
  showToast: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
}));

vi.mock("@/lib/api", () => ({
  lostItemApi: {
    getMyTrackedItems,
    stopTracking: vi.fn(),
  },
}));

const user = { accessToken: "test-access-token" };

vi.mock("@/context/AppContext", () => ({
  useApp: () => ({
    lang: "ko",
    setLang: vi.fn(),
    t: {
      trackingTitle: "내 추적",
      tabActive: "진행 중",
      tabCompleted: "종료됨",
      noCandidates: "후보 0건",
      registeredAt: "등록일",
      expiresAt: "종료 예정일",
      endTrackingBtn: "추적 종료",
    },
    user,
    stopTracking: vi.fn(),
    showToast,
  }),
}));

function trackedItem(id, conditions, overrides = {}) {
  return {
    lostItemId: id,
    description: `설명 ${id}`,
    languageCode: "ko",
    conditions,
    status: "TRACKING",
    notificationEmail: "user@example.test",
    startedAt: "2026-09-22T15:00:00.000000+09:00",
    expiresAt: "2026-09-29T15:00:00.000000+09:00",
    lastAutoSearchDate: "2026-09-22",
    currentCandidateCount: 0,
    createdAt: "2026-09-22T15:00:00.000000+09:00",
    updatedAt: "2026-09-22T15:00:00.000000+09:00",
    currentCandidate: null,
    ...overrides,
  };
}

function mockTrackedItems(items) {
  getMyTrackedItems.mockResolvedValue({ items, page: 0, size: 50, totalElements: items.length, totalPages: 1 });
}

describe("TrackingPage API-16 registered item display", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows itemTypeName and colorName instead of official codes", async () => {
    mockTrackedItems([
      trackedItem("1", {
        categoryLargeCode: null,
        categoryMiddleCode: null,
        colorCode: null,
        itemTypeName: "핸드폰",
        colorName: "검정",
      }),
    ]);

    render(<TrackingPage />);

    expect(await screen.findByText("핸드폰")).toBeInTheDocument();
    expect(screen.getByText("검정")).toBeInTheDocument();
    expect(screen.queryByText("기타")).not.toBeInTheDocument();
    expect(screen.queryByText("미지정")).not.toBeInTheDocument();
    expect(getMyTrackedItems).toHaveBeenCalledWith("test-access-token", 0, 50);
  });

  it("shows 미지정 for null, undefined and blank values without falling back to codes", async () => {
    mockTrackedItems([
      trackedItem("1", { categoryLargeCode: "PRA000", colorCode: "CL1001", itemTypeName: null, colorName: "   " }),
      trackedItem("2", { categoryLargeCode: null, colorCode: null }),
      trackedItem("3", undefined),
    ]);

    render(<TrackingPage />);

    expect(await screen.findAllByText("미지정")).toHaveLength(6);
    expect(screen.queryByText("PRA000")).not.toBeInTheDocument();
    expect(screen.queryByText("CL1001")).not.toBeInTheDocument();
    expect(screen.queryByText("기타")).not.toBeInTheDocument();
  });

  it("does not use currentCandidate as the registered item display values", async () => {
    mockTrackedItems([
      trackedItem(
        "1",
        { categoryLargeCode: null, colorCode: null, itemTypeName: "지갑", colorName: null },
        {
          currentCandidateCount: 1,
          currentCandidate: { foundItem: { categoryName: "휴대폰 > 스마트폰", colorName: "흰색" } },
        },
      ),
    ]);

    render(<TrackingPage />);

    expect(await screen.findByText("지갑")).toBeInTheDocument();
    expect(screen.getByText("미지정")).toBeInTheDocument();
    expect(screen.queryByText("휴대폰 > 스마트폰")).not.toBeInTheDocument();
    expect(screen.queryByText("흰색")).not.toBeInTheDocument();
  });
});
