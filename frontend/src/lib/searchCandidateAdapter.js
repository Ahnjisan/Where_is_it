export const adaptSearchCandidate = (candidate = {}) => {
  const foundItem = candidate.foundItem || {};
  const id = candidate.candidateId ?? foundItem.foundItemId ?? null;
  const imageUrl = foundItem.imageUrl ?? "";

  const rank = Number.isFinite(candidate.rank) ? candidate.rank : null;

  // 일치율(matchRate) 계산:
  // 1) candidate.score 또는 candidate.matchRate가 있으면 사용
  // 2) 서버 rank가 있을 때 1위(96%), 2위(92%), 3위(88%), 4위(84%)... 순차 감소 (최소 60%)
  // 3) isSimilar 여부 반영
  let matchRate = null;
  if (Number.isFinite(candidate.matchRate)) {
    matchRate = candidate.matchRate;
  } else if (Number.isFinite(candidate.score)) {
    matchRate = candidate.score;
  } else if (rank != null && rank > 0) {
    matchRate = Math.max(60, 96 - (rank - 1) * 4);
    if (candidate.isSimilar === false) {
      matchRate = Math.max(50, matchRate - 15);
    }
  }

  return {
    id,
    candidateId: candidate.candidateId ?? null,
    foundItemId: foundItem.foundItemId ?? null,
    rank,
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
    matchRate,
    isSimilar: candidate.isSimilar ?? true,
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
