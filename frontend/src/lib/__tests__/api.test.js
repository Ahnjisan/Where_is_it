import { afterEach, describe, expect, it, vi } from "vitest";
import { authApi, lostItemApi } from "../api";

const response = ({ ok = true, status = 200, body }) => ({
  ok,
  status,
  text: vi.fn().mockResolvedValue(body),
});

describe("lostItemApi.createSearch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the API-05 body and bearer token and unwraps data", async () => {
    const data = { lookupStatus: "COMPLETE", candidates: [] };
    const fetchMock = vi.fn().mockResolvedValue(
      response({ body: JSON.stringify({ success: true, data }) }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      lostItemApi.createSearch(
        { description: "파란 지갑", languageCode: "ko" },
        "access-token",
      ),
    ).resolves.toEqual(data);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/lost-items",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        }),
        body: JSON.stringify({ description: "파란 지갑", languageCode: "ko" }),
      }),
    );
  });

  it("preserves the backend error envelope", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        response({
          ok: false,
          status: 502,
          body: JSON.stringify({
            success: false,
            error: { code: "LOST_API_UNAVAILABLE", message: "조회 실패" },
          }),
        }),
      ),
    );

    await expect(
      lostItemApi.createSearch({ description: "지갑", languageCode: "ko" }, "token"),
    ).rejects.toMatchObject({
      message: "조회 실패",
      status: 502,
      code: "LOST_API_UNAVAILABLE",
    });
  });

  it("handles non-JSON and network failures", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(response({ status: 502, body: "gateway failure" })),
    );
    await expect(
      lostItemApi.createSearch({ description: "지갑", languageCode: "ko" }, "token"),
    ).rejects.toMatchObject({ code: "INVALID_API_RESPONSE" });

    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("network down")));
    await expect(
      lostItemApi.createSearch({ description: "지갑", languageCode: "ko" }, "token"),
    ).rejects.toThrow("network down");
  });

  it("keeps the existing auth API envelope behavior", async () => {
    const envelope = { success: true, data: { accessToken: "token" } };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(response({ body: JSON.stringify(envelope) })),
    );

    await expect(authApi.login({ email: "user@example.test", password: "pw" }))
      .resolves.toEqual(envelope);
  });
});
