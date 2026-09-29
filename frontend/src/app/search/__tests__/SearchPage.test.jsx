import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import SearchPage from "../page";

const push = vi.fn();
const showToast = vi.fn();
let searchExecution = null;

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, back: vi.fn() }),
  useSearchParams: () => ({ get: () => "파란 지갑" }),
}));

vi.mock("next/link", () => ({
  default: ({ children, ...props }) => <a {...props}>{children}</a>,
}));

vi.mock("@/context/AppContext", () => ({
  useApp: () => ({
    lang: "ko",
    t: {
      searchResults: "검색 결과",
      searchExpiredTitle: "검색 결과가 만료되었습니다. 다시 검색해 주세요.",
      searchGoBack: "검색 화면으로 돌아가기",
      sortRecommend: "추천순",
      sortLatest: "최신순",
      filterAdjust: "필터",
      resultCountPrefix: "총",
      resultCountSuffix: "건",
      trackRegisterCta: "추적 등록",
    },
    searchExecution,
    addTracking: vi.fn(),
    showToast,
  }),
}));

describe("SearchPage API-05 results", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
    searchExecution = null;
  });

  it("shows an expiration message instead of mock data when context is empty", () => {
    render(<SearchPage />);
    expect(screen.getByText("검색 결과가 만료되었습니다. 다시 검색해 주세요.")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "검색 화면으로 돌아가기" }));
    expect(push).toHaveBeenCalledWith("/");
  });

  it("renders actual candidates in rank order and handles card click", () => {
    searchExecution = {
      lookupStatus: "COMPLETE",
      warnings: [],
      candidates: [
        candidate("mock이 아닌 두 번째 후보", 2, "2"),
        candidate("실제 첫 번째 후보", 1, "1"),
      ],
    };
    render(<SearchPage />);

    const names = screen.getAllByRole("heading", { level: 3 }).map((heading) => heading.textContent);
    expect(names).toEqual(["실제 첫 번째 후보", "mock이 아닌 두 번째 후보"]);
    expect(screen.queryByText(/일치율/)).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("실제 첫 번째 후보"));
    expect(push).toHaveBeenCalledWith("/items/candidate-1");
  });

  it("shows PARTIAL warnings and the empty-result UI", () => {
    searchExecution = {
      lookupStatus: "PARTIAL",
      warnings: ["PARTIAL_SOURCE_RESULT"],
      candidates: [],
    };
    render(<SearchPage />);

    expect(screen.getByText("일부 기관 조회에 실패했습니다. 제공 가능한 결과만 표시합니다.")).toBeInTheDocument();
    expect(screen.getByText("PARTIAL_SOURCE_RESULT")).toBeInTheDocument();
    expect(screen.getByText("일치하는 습득물이 없습니다.")).toBeInTheDocument();
  });
});

function candidate(name, rank, foundItemId) {
  return {
    candidateId: `candidate-${foundItemId}`,
    rank,
    reason: "규칙 기반 추천",
    foundItem: {
      foundItemId,
      sourceType: "POLICE",
      atcId: `atc-${foundItemId}`,
      fdSn: foundItemId,
      productName: name,
      foundDate: "2026-09-20",
      storagePlace: "서울역 유실물센터",
      imageUrl: null,
    },
  };
}
