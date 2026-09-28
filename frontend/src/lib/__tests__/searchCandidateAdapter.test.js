import { describe, expect, it } from "vitest";
import { adaptSearchCandidate, adaptSearchCandidates } from "../searchCandidateAdapter";

describe("searchCandidateAdapter", () => {
  it("maps found-item fields without inventing a match rate", () => {
    const result = adaptSearchCandidate({
      candidateId: "10",
      rank: 2,
      reason: "설명 일치",
      foundItem: {
        foundItemId: "20",
        sourceType: "POLICE",
        atcId: "A",
        fdSn: "1",
        productName: "파란 지갑",
        foundDate: "2026-09-20",
        foundPlace: "서울역",
        storagePlace: "서울역 유실물센터",
        categoryName: "지갑",
        colorName: "파랑",
        imageUrl: "https://example.test/item.jpg",
        description: "카드가 들어 있음",
        storagePhone: "02-0000-0000",
      },
    });

    expect(result).toMatchObject({
      id: "10",
      foundItemId: "20",
      name: "파란 지갑",
      images: ["https://example.test/item.jpg"],
      matchRate: null,
      location: "서울역",
      storageFacility: "서울역 유실물센터",
    });
  });

  it("uses safe fallbacks for a transient candidate with null detail fields", () => {
    const result = adaptSearchCandidate({
      candidateId: null,
      rank: 1,
      foundItem: {
        foundItemId: "21",
        subject: "습득물 제목",
        storagePlace: "보관소",
        imageUrl: null,
      },
    });

    expect(result).toMatchObject({
      id: "21",
      candidateId: null,
      name: "습득물 제목",
      location: "보관소",
      category: "기타",
      images: [],
      image: "",
      description: "습득물 제목",
      phone: "",
    });
  });

  it("sorts ranks ascending and puts missing ranks last", () => {
    const candidates = adaptSearchCandidates([
      { rank: null, foundItem: { foundItemId: "none" } },
      { rank: 3, foundItem: { foundItemId: "three" } },
      { rank: 1, foundItem: { foundItemId: "one" } },
    ]);

    expect(candidates.map((candidate) => candidate.id)).toEqual(["one", "three", "none"]);
  });
});
