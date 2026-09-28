import { expect, test } from "@playwright/test";

const DESCRIPTION = "I lost a black wallet near Seoul Station.";
const ASSISTANT_MESSAGE =
  "I found two deterministic test candidates near Seoul Station.";
const RANK_ONE_REASON =
  "The item, color, and Seoul Station location match.";
const RANK_TWO_REASON = "The color and item type match the description.";
const TEST_PASSWORD = "E2eOnly!89";

const isApiResponse = (response, pathname) => {
  const url = new URL(response.url());
  return url.pathname === pathname;
};

test("signs up, signs in with a fresh context, and shows ranked API-05 candidates", async ({
  browser,
}) => {
  const email = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 10)}@example.test`;

  await test.step("sign up through the UI in the first browser context", async () => {
    const signupContext = await browser.newContext();
    const signupPage = await signupContext.newPage();

    try {
      await signupPage.goto("/signup");

      await signupPage.getByPlaceholder("이메일 주소").fill(email);
      await signupPage.getByPlaceholder("비밀번호 (8~20자)").fill(TEST_PASSWORD);
      await signupPage.getByPlaceholder("비밀번호 확인").fill(TEST_PASSWORD);
      await signupPage
        .getByText("이용약관 및 개인정보 처리방침에 동의합니다.", { exact: true })
        .locator("xpath=preceding-sibling::div")
        .click();

      const signupResponsePromise = signupPage.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          isApiResponse(response, "/api/auth/signup"),
      );
      const automaticLoginResponsePromise = signupPage.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          isApiResponse(response, "/api/auth/login"),
      );

      await signupPage.getByRole("button", { name: "회원가입", exact: true }).click();

      expect((await signupResponsePromise).status()).toBe(201);
      expect((await automaticLoginResponsePromise).status()).toBe(200);
      await expect(signupPage).toHaveURL("/");
    } finally {
      await signupContext.close();
    }
  });

  const context = await browser.newContext();
  const page = await context.newPage();
  let api05RequestCount = 0;
  let unexpectedExternalRequestCount = 0;

  page.on("request", (request) => {
    const url = new URL(request.url());
    if (request.method() === "POST" && url.pathname === "/api/lost-items") {
      api05RequestCount += 1;
    }
    if (url.protocol !== "data:" && !["127.0.0.1", "localhost"].includes(url.hostname)) {
      unexpectedExternalRequestCount += 1;
    }
  });

  try {
    await test.step("sign in through the UI in a fresh browser context", async () => {
      expect(await context.cookies()).toHaveLength(0);
      await page.goto("/signin");
      expect(await page.evaluate(() => localStorage.getItem("where_user"))).toBeNull();

      await page.getByPlaceholder("이메일 주소").fill(email);
      await page.getByPlaceholder("비밀번호 (8자 이상)").fill(TEST_PASSWORD);

      const loginResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          isApiResponse(response, "/api/auth/login"),
      );
      await page.getByRole("button", { name: "로그인", exact: true }).click();

      expect((await loginResponsePromise).status()).toBe(200);
      await expect(page).toHaveURL("/");
    });

    await test.step("switch to English through My Page", async () => {
      await page.getByRole("link", { name: "마이", exact: true }).click();
      await expect(page).toHaveURL("/my");

      await page.getByRole("button", { name: "한국어", exact: true }).click();
      const languageResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          isApiResponse(response, "/api/members/me"),
      );
      await page.getByRole("button", { name: /^English/ }).click();

      const languageResponse = await languageResponsePromise;
      expect(languageResponse.status()).toBe(200);
      expect(languageResponse.request().postDataJSON().languageCode).toBe("en");

      await page.getByRole("link", { name: "Home", exact: true }).click();
      await expect(page).toHaveURL("/");
      await expect(
        page.getByPlaceholder("Please describe what you lost in detail..."),
      ).toBeVisible();
    });

    let api05Data;
    await test.step("submit API-05 once and validate its HTTP contract", async () => {
      const searchInput = page.getByPlaceholder(
        "Please describe what you lost in detail...",
      );
      await searchInput.fill(DESCRIPTION);

      const api05ResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          isApiResponse(response, "/api/lost-items"),
      );

      await page.getByRole("button", { name: "전송", exact: true }).click();
      await searchInput.press("Enter");

      await expect(page.getByText("Searching...", { exact: true })).toBeVisible();
      await expect(page.getByRole("button", { name: "전송", exact: true })).toBeDisabled();

      const api05Response = await api05ResponsePromise;
      expect(api05Response.status()).toBe(201);
      const requestData = api05Response.request().postDataJSON();
      expect(requestData.description).toBe(DESCRIPTION);
      expect(requestData.languageCode).toBe("en");

      const body = await api05Response.json();
      expect(body.success).toBe(true);
      api05Data = body.data;

      expect(api05Data.lostItem.lostItemId).toEqual(expect.any(String));
      expect(api05Data.lostItem.lostItemId.trim()).not.toBe("");
      expect(api05Data.assistantMessage).toBeTruthy();
      expect(api05Data.assistantMessage.content).toBe(ASSISTANT_MESSAGE);
      expect(api05Data.lookupStatus).toBe("COMPLETE");
      expect(api05Data.rankingStatus).toBe("SUCCESS");
      expect(api05Data.persisted).toBe(false);
      expect(api05Data.warnings).toEqual([]);
      expect(api05Data.candidates).toHaveLength(2);
      expect(api05Data.candidates.map((candidate) => candidate.rank).sort()).toEqual([1, 2]);

      const rankOne = api05Data.candidates.find((candidate) => candidate.rank === 1);
      const rankTwo = api05Data.candidates.find((candidate) => candidate.rank === 2);
      expect(rankOne.reason).toBe(RANK_ONE_REASON);
      expect(rankTwo.reason).toBe(RANK_TWO_REASON);

      for (const candidate of api05Data.candidates) {
        expect(candidate.candidateId).toBeNull();
        expect(candidate.isCurrent).toBe(false);
        expect(candidate.isBaseline).toBe(false);
        expect(candidate.foundItem.openId).toBe(candidate.foundItem.atcId);
      }

      expect(rankOne.foundItem.fdSn).toBe("0001");
      expect(rankTwo.foundItem.fdSn).toBe("0002");
      expect(api05RequestCount).toBe(1);
    });

    await test.step("show the current candidate-card fields in rank order", async () => {
      await expect(page).toHaveURL((url) =>
        url.pathname === "/search" && url.searchParams.get("q") === DESCRIPTION,
      );
      await expect(page.getByRole("heading", { name: "Search Results" })).toBeVisible();

      const productHeadings = page.locator("h3");
      await expect(productHeadings).toHaveCount(2);
      await expect(productHeadings.nth(0)).toHaveText("Dummy Black Wallet A");
      await expect(productHeadings.nth(1)).toHaveText("Dummy Black Wallet B");

      await expect(page.getByText("2026-09-21", { exact: true })).toBeVisible();
      await expect(page.getByText("2026-09-22", { exact: true })).toBeVisible();
      await expect(
        page.getByText("Dummy Seoul Station Information Desk", { exact: true }),
      ).toBeVisible();
      await expect(page.getByText("Dummy Seoul Station Storage", { exact: true })).toBeVisible();
      await expect(page.getByText("Wallet", { exact: true })).toHaveCount(2);
      await expect(page.getByText("Black", { exact: true })).toHaveCount(2);

      const rankOneHeading = page.getByRole("heading", {
        name: "Dummy Black Wallet A",
        exact: true,
      });
      const rankTwoHeading = page.getByRole("heading", {
        name: "Dummy Black Wallet B",
        exact: true,
      });
      const rankOneCard = rankOneHeading.locator("xpath=../..");
      const rankTwoCard = rankTwoHeading.locator("xpath=../..");
      await expect(rankOneCard.getByRole("img", { name: "Dummy Black Wallet A" })).toBeVisible();
      await expect(rankTwoCard.locator("img")).toHaveCount(0);
      await expect(rankTwoCard.locator(":scope > div").first()).toBeVisible();

      const visibleText = await page.locator("body").innerText();
      expect(visibleText).not.toContain("null");
      expect(visibleText).not.toContain("undefined");
      expect(visibleText).not.toContain("[object Object]");
      expect(visibleText).not.toMatch(/\b\d+(?:\.\d+)?\s*%/);
      expect(api05RequestCount).toBe(1);
      expect(unexpectedExternalRequestCount).toBe(0);
    });
  } finally {
    await context.close();
  }
});
