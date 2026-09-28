import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import HomePage from "../page";

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  createSearch: vi.fn(),
  setSearchExecution: vi.fn(),
  clearSearchExecution: vi.fn(),
  showToast: vi.fn(),
  logoutUser: vi.fn().mockResolvedValue(undefined),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mocks.push }),
}));

vi.mock("next/link", () => ({
  default: ({ children, ...props }) => <a {...props}>{children}</a>,
}));

vi.mock("@/lib/api", () => ({
  lostItemApi: { createSearch: mocks.createSearch },
}));

vi.mock("@/context/AppContext", () => ({
  useApp: () => ({
    lang: "ko",
    t: {
      mainQuestion: "무엇을 잃어버렸나요?",
      mainSub: "설명해 주세요",
      examplePromptLabel: "예시",
      searchPlaceholder: "분실물을 설명해 주세요",
      enterToSearch: "Enter로 검색",
    },
    showToast: mocks.showToast,
    user: { accessToken: "access-token" },
    logoutUser: mocks.logoutUser,
    setSearchExecution: mocks.setSearchExecution,
    clearSearchExecution: mocks.clearSearchExecution,
  }),
}));

vi.mock("@/lib/mockData", () => ({
  EXAMPLE_PROMPTS_BY_LANG: { ko: [], en: [] },
}));

describe("HomePage API-05 search", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("does not call the API for an empty query", () => {
    render(<HomePage />);
    expect(screen.getByRole("button", { name: "전송" })).toBeDisabled();
    expect(mocks.createSearch).not.toHaveBeenCalled();
  });

  it("prevents duplicate submission, shows loading, stores data, and navigates", async () => {
    let resolveSearch;
    mocks.createSearch.mockReturnValue(
      new Promise((resolve) => {
        resolveSearch = resolve;
      }),
    );
    const result = { lookupStatus: "COMPLETE", candidates: [] };
    render(<HomePage />);
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "  파란 지갑  " } });

    const submit = screen.getByRole("button", { name: "전송" });
    fireEvent.click(submit);
    fireEvent.click(submit);

    expect(mocks.createSearch).toHaveBeenCalledTimes(1);
    expect(mocks.createSearch).toHaveBeenCalledWith(
      { description: "파란 지갑", languageCode: "ko" },
      "access-token",
    );
    expect(submit).toBeDisabled();
    expect(screen.getByText("검색 중...")).toBeInTheDocument();

    resolveSearch(result);
    await waitFor(() => expect(mocks.setSearchExecution).toHaveBeenCalledWith(result));
    expect(mocks.push).toHaveBeenCalledWith("/search?q=%ED%8C%8C%EB%9E%80%20%EC%A7%80%EA%B0%91");
  });

  it("clears authentication and redirects on 401", async () => {
    mocks.createSearch.mockRejectedValue(Object.assign(new Error("expired"), { status: 401 }));
    render(<HomePage />);
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "지갑" } });
    fireEvent.click(screen.getByRole("button", { name: "전송" }));

    await waitFor(() => expect(mocks.logoutUser).toHaveBeenCalled());
    expect(mocks.push).toHaveBeenCalledWith("/signin");
  });
});
