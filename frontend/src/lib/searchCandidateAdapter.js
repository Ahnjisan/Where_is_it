export const adaptSearchCandidate = (candidate = {}) => {
  const foundItem = candidate.foundItem || {};
  const id = candidate.candidateId ?? foundItem.foundItemId ?? null;
  const imageUrl = foundItem.imageUrl ?? "";

  return {
    id,
    candidateId: candidate.candidateId ?? null,
    foundItemId: foundItem.foundItemId ?? null,
    rank: Number.isFinite(candidate.rank) ? candidate.rank : null,
    name: foundItem.productName ?? foundItem.subject ?? "물품명 없음",
    date: foundItem.foundDate ?? null,
    location: foundItem.foundPlace ?? foundItem.storagePlace ?? null,
    storageFacility: foundItem.storagePlace ?? foundItem.foundPlace ?? null,
    category: foundItem.categoryName ?? "기타",
    color: foundItem.colorName ?? null,
    image: imageUrl,
    images: imageUrl ? [imageUrl] : [],
    description: foundItem.description ?? foundItem.subject ?? "",
    phone: foundItem.storagePhone ?? "",
    matchRate: null,
    sourceType: foundItem.sourceType ?? null,
    atcId: foundItem.atcId ?? null,
    fdSn: foundItem.fdSn ?? null,
    reason: candidate.reason ?? "",
  };
};

export const adaptSearchCandidates = (candidates = []) =>
  candidates
    .map(adaptSearchCandidate)
    .sort((left, right) => {
      if (left.rank == null) return right.rank == null ? 0 : 1;
      if (right.rank == null) return -1;
      return left.rank - right.rank;
    });
