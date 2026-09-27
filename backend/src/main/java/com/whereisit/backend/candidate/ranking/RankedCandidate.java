package com.whereisit.backend.candidate.ranking;

import com.whereisit.backend.founditem.entity.FoundItem;

public record RankedCandidate(FoundItem foundItem, int rank, String reason, boolean similar) {
}
