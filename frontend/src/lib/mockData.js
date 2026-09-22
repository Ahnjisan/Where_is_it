// 분실물 데이터 및 다국어 텍스트 정의

export const INITIAL_LOST_ITEMS = [
  {
    id: "item-1",
    name: "가죽 반지갑",
    matchRate: 98,
    date: "2025-09-14",
    location: "서울역 (서울특별시)",
    storageFacility: "서울역 분실물 보관소",
    category: "지갑",
    color: "검정색",
    description:
      "겉면에 가죽 지갑입니다. 내부에 신분증 및 카드가 여러 장 들어있습니다.",
    phone: "02-3149-2470",
    operatingHours: "평일 06:00 ~ 23:00 / 주말 09:00 ~ 18:00",
    images: [
      "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: false,
  },
  {
    id: "item-2",
    name: "슬림 사피아노 반지갑",
    matchRate: 92,
    date: "2025-09-13",
    location: "시청역 (서울특별시)",
    storageFacility: "서울교통공사 유실물센터(시청)",
    category: "지갑",
    color: "남색",
    description:
      "남색 가죽 소재의 반지갑으로 소액 현금과 체크카드가 보관되어 있습니다.",
    phone: "02-6110-1122",
    operatingHours: "평일 09:00 ~ 18:00 (공휴일 휴무)",
    images: [
      "https://images.unsplash.com/photo-1606503829068-1ebef6340333?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: false,
  },
  {
    id: "item-3",
    name: "목걸이형 카드홀더",
    matchRate: 88,
    date: "2025-09-12",
    location: "사당역 (서울특별시)",
    storageFacility: "사당역 역무실",
    category: "지갑",
    color: "검정색",
    description:
      "슬림한 목걸이형 카드홀더입니다. 사원증과 교통카드가 수납되어 있습니다.",
    phone: "02-6110-4331",
    operatingHours: "07:00 ~ 23:00 (연중무휴)",
    images: [
      "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: true,
  },
  {
    id: "item-4",
    name: "빈티지 브라운 장지갑",
    matchRate: 76,
    date: "2025-09-11",
    location: "강남구청 (서울특별시)",
    storageFacility: "강남경찰서 생활질서계",
    category: "지갑",
    color: "갈색",
    description: "빈티지 브라운 장지갑입니다. 명함 및 영수증이 들어있습니다.",
    phone: "02-3484-8344",
    operatingHours: "평일 09:00 ~ 18:00",
    images: [
      "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: false,
  },
  {
    id: "item-5",
    name: "아이폰 14 프로 실버",
    matchRate: 95,
    date: "2025-09-15",
    location: "강남역 2번 출구 부근",
    storageFacility: "역삼지구대",
    category: "휴대폰",
    color: "흰색",
    description:
      "투명 범퍼 케이스 착용된 스마트폰. 잠금화면에 고양이 배경화면 설정됨.",
    phone: "02-567-0112",
    operatingHours: "24시간 운영",
    images: [
      "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: false,
  },
  {
    id: "item-6",
    name: "비즈니스 방수 백팩",
    matchRate: 90,
    date: "2025-09-13",
    location: "2호선 열차 내 (신도림역 방면)",
    storageFacility: "시청역 유실물센터",
    category: "가방",
    color: "회색",
    description:
      "생활 방수 원단의 회색 비즈니스 백팩. 내부에 파우치와 텀블러 보관 중.",
    phone: "02-6110-1122",
    operatingHours: "평일 09:00 ~ 18:00",
    images: [
      "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80",
      "https://images.unsplash.com/photo-1546938576-6e6a64f317cc?auto=format&fit=crop&w=800&q=80",
    ],
    isLiked: false,
  },
];

export const INITIAL_TRACKING_LIST = {
  active: [
    {
      id: "track-1",
      title: "가죽 반지갑",
      name: "가죽 반지갑",
      prompt: "서울역 1호선 승강장에서 검은색 프라다 사피아노 가죽 반지갑을 두고 내렸습니다.",
      registeredDate: "2025-09-10",
      endDate: "2025-09-17",
      dDay: "D-3",
      candidatesCount: 2,
      category: "지갑",
      color: "검정색",
      location: "서울역 1호선 승강장",
    },
    {
      id: "track-2",
      title: "비즈니스 백팩",
      name: "비즈니스 백팩",
      prompt: "출근길 지하철 2호선 홍대입구역 방면 열차 선반 위에 둔 회색 노트북 백팩입니다.",
      registeredDate: "2025-09-11",
      endDate: "2025-09-18",
      dDay: "D-4",
      candidatesCount: 0,
      category: "가방",
      color: "회색",
      location: "지하철 2호선",
    },
    {
      id: "track-3",
      title: "아이폰 14 프로",
      name: "아이폰 14 프로",
      prompt: "강남역 11번 출구 부근 버스 정류장 벤치에서 분실한 흰색(실버) 아이폰입니다.",
      registeredDate: "2025-09-12",
      endDate: "2025-09-19",
      dDay: "D-5",
      candidatesCount: 1,
      category: "전자기기",
      color: "흰색",
      location: "강남역 11번 출구",
    },
  ],
  completed: [
    {
      id: "track-4",
      title: "에어팟 프로 2세대",
      name: "에어팟 프로 2세대",
      prompt: "홍대입구역 인근 카페 테이블에 두고 온 흰색 에어팟 프로 2세대 본체입니다.",
      registeredDate: "2025-08-15",
      endDate: "2025-08-22",
      statusText: "수령 완료",
      candidatesCount: 3,
      category: "전자기기",
      color: "흰색",
      location: "홍대입구역 카페",
    },
    {
      id: "track-5",
      title: "슬림 여권 케이스",
      name: "슬림 여권 케이스",
      prompt: "인천공항 제1여객터미널 출국장 탑승 대기 벤치에서 분실한 남색 가죽 여권 케이스입니다.",
      registeredDate: "2025-07-01",
      endDate: "2025-07-15",
      statusText: "기한 만료",
      candidatesCount: 0,
      category: "지갑",
      color: "남색",
      location: "인천공항 T1 출국장",
    },
  ],
};

export const EXAMPLE_PROMPTS = [
  "서울역에서 검은 지갑을 잃어버렸어요",
  "어제 강남에서 아이폰을 분실했어요",
  "지하철에서 가방을 놓고 내렸어요",
];

export const CATEGORIES = [
  "전체",
  "가방",
  "귀금속",
  "도서용품",
  "서류",
  "산업용품",
  "쇼핑백",
  "스포츠용품",
  "악기",
  "유가증권",
  "의류",
  "자동차",
  "전자기기",
  "지갑",
  "증명서",
  "컴퓨터",
  "카드",
  "현금",
  "휴대폰",
  "기타물품",
];
export const COLORS = [
  "전체",
  "검정색",
  "흰색",
  "남색",
  "갈색",
  "회색",
  "빨간색",
  "기타",
];
export const REGIONS = [
  "전체",
  "서울",
  "경기",
  "인천",
  "부산",
  "대구",
  "대전",
  "광주",
  "제주",
];

// 다국어 사전 (한국어 / 영어)
export const I18N = {
  ko: {
    appName: "어디갔지",
    appSubSignup: "한국에서 잃어버린 소중한 물건, AI와 함께 찾아요.",
    appSubLogin: "언제 어디서나, 당신의 물건을 다시 찾아요.",
    signup: "회원가입",
    login: "로그인",
    emailPlaceholder: "이메일 주소",
    pwPlaceholderSignup: "비밀번호 (8자 이상)",
    pwPlaceholderLogin: "비밀번호",
    pwConfirmPlaceholder: "비밀번호 확인",
    languageSelect: "사용 언어",
    agreeTerms: "이용약관 및 개인정보 처리방침에 동의합니다.",
    alreadyHaveAccount: "이미 계정이 있으신가요? 로그인",
    noAccount: "계정이 없으신가요? 회원가입",
    keepLoggedIn: "로그인 상태 유지",
    findPassword: "비밀번호 찾기",

    // 메인
    mainQuestion: "어떤 물건을 잃어버리셨나요?",
    mainSub:
      "자연어로 자유롭게 설명해 주세요. AI가 한국의 습득물 정보를 찾아드립니다.",
    examplePromptLabel: "예시로 질문해보세요",
    searchPlaceholder: "잃어버린 물건에 대해 자세히 설명해 주세요...",

    // 네비게이션
    navHome: "홈",
    navSearch: "검색",
    navTrack: "내 추적",
    navAlert: "알림",
    navMy: "마이",

    // 검색 결과
    searchResults: "검색 결과",
    sortRecommend: "추천순",
    sortLatest: "최신순",
    resultCountPrefix: "총",
    resultCountSuffix: "개의 유사한 습득물 후보가 있습니다.",
    matchSuffix: "일치",
    filterAdjust: "조건 보정",
    trackRegisterCta: "내 분실물이 없나요? 추적 등록하기",

    // 상세
    storageDate: "보관일자",
    foundLocation: "습득장소",
    storageFacility: "보관 장소",
    categoryLabel: "분류",
    colorLabel: "색상",
    facilityPhone: "기관 전화번호",
    detailDesc: "분실물 상세 설명",
    contactFacility: "보관기관 문의하기",

    // 조건 보정 모달
    filterTitle: "검색 조건 보정",
    tabNaturalLanguage: "자연어 보정",
    tabDetailFilter: "상세 필터",
    nlPromptHeader: "더 정확한 검색을 위해 조건을 추가해보세요.",
    nlPromptPlaceholder: "예) 강남역 부근에서 잃어버린 검정 지갑",
    nlApplyBtn: "자연어로 조건 보정하기",
    manualFilterHeader: "직접 설정하기",
    lostDate: "분실한 날짜",
    applyFilterBtn: "필터 적용하기",

    // 추적 관리
    trackingTitle: "내 분실물 추적",
    tabActive: "진행 중",
    tabCompleted: "종료됨",
    registeredAt: "등록일",
    expiresAt: "추적 종료",
    additionalCandidates: "추가 후보",
    editBtn: "수정",

    // 알림 토스트
    toastTrackingSuccess:
      "분실물 추적 등록이 완료되었습니다! 신규 습득물이 등록되면 알림을 드려요.",
    toastFacilityCall:
      "보관기관 연락처 및 운영시간이 클립보드에 복사되었습니다.",
    toastLiked: "관심 분실물에 저장되었습니다.",
    toastUnliked: "관심 분실물에서 제외되었습니다.",
  },
  en: {
    appName: "Where Is It",
    appSubSignup: "Find your precious belongings lost in Korea with AI.",
    appSubLogin: "Anytime, anywhere, reunite with your lost items.",
    signup: "Sign Up",
    login: "Log In",
    emailPlaceholder: "Email address",
    pwPlaceholderSignup: "Password (8+ chars)",
    pwPlaceholderLogin: "Password",
    pwConfirmPlaceholder: "Confirm password",
    languageSelect: "Language",
    agreeTerms: "I agree to the Terms of Service & Privacy Policy.",
    alreadyHaveAccount: "Already have an account? Log In",
    noAccount: "Don't have an account? Sign Up",
    keepLoggedIn: "Keep me signed in",
    findPassword: "Forgot password?",

    // 메인
    mainQuestion: "What did you lose?",
    mainSub:
      "Describe it naturally in your own words. AI searches found item records across Korea.",
    examplePromptLabel: "Try asking like this",
    searchPlaceholder: "Please describe what you lost in detail...",

    // 네비게이션
    navHome: "Home",
    navSearch: "Search",
    navTrack: "Tracking",
    navAlert: "Alerts",
    navMy: "My Page",

    // 검색 결과
    searchResults: "Search Results",
    sortRecommend: "Recommended",
    sortLatest: "Latest",
    resultCountPrefix: "Found",
    resultCountSuffix: "similar item candidates.",
    matchSuffix: "Match",
    filterAdjust: "Adjust Filter",
    trackRegisterCta: "Can't find your item? Start Tracking",

    // 상세
    storageDate: "Storage Date",
    foundLocation: "Found Location",
    storageFacility: "Holding Place",
    categoryLabel: "Category",
    colorLabel: "Color",
    facilityPhone: "Facility Phone",
    detailDesc: "Item Description",
    contactFacility: "Contact Facility",

    // 조건 보정 모달
    filterTitle: "Refine Search Conditions",
    tabNaturalLanguage: "Natural Language",
    tabDetailFilter: "Detailed Filters",
    nlPromptHeader: "Add more details to find your exact match.",
    nlPromptPlaceholder: "e.g. Black wallet lost near Gangnam Station",
    nlApplyBtn: "Search with Natural Language",
    manualFilterHeader: "Manual Filter Settings",
    lostDate: "Date Lost",
    applyFilterBtn: "Apply Filters",

    // 추적 관리
    trackingTitle: "Lost Item Tracking",
    tabActive: "In Progress",
    tabCompleted: "Completed",
    registeredAt: "Registered",
    expiresAt: "Expires",
    additionalCandidates: "New candidates",
    editBtn: "Edit",

    // 알림 토스트
    toastTrackingSuccess:
      "Tracking activated! We'll notify you as soon as a matching item is found.",
    toastFacilityCall: "Facility phone & info copied to clipboard.",
    toastLiked: "Saved to your favorites.",
    toastUnliked: "Removed from favorites.",
  },
};
