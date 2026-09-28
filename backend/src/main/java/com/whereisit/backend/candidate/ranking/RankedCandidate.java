package com.whereisit.backend.candidate.ranking;

public record RankedCandidate(String candidateKey, int rank, String reason, boolean similar) {
}
