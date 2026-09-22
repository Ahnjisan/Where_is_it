import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import BrandLogo from "../BrandLogo";

describe("BrandLogo", () => {
  it("renders the Korean title by default", () => {
    render(<BrandLogo />);
    expect(screen.getByText("어디갔지")).toBeInTheDocument();
  });

  it("renders the English title when lang is not ko", () => {
    render(<BrandLogo lang="en" />);
    expect(screen.getByText("Where Is It")).toBeInTheDocument();
  });

  it("renders the subtitle when provided", () => {
    render(<BrandLogo subtitle="잃어버린 물건을 찾아드립니다" />);
    expect(
      screen.getByText("잃어버린 물건을 찾아드립니다")
    ).toBeInTheDocument();
  });
});
